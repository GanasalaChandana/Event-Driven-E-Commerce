import requests
import asyncio
import os
import sys
import time
from mcp.server import Server
from mcp.server.stdio import stdio_server
from mcp.types import Tool, TextContent

SHOPFLOW_API = os.environ.get("SHOPFLOW_API_URL", "https://event-driven-e-commerce.onrender.com") .rstrip("/") + "/api/v1"
ADMIN_EMAIL = os.environ.get("SHOPFLOW_ADMIN_EMAIL", "admin@shopflow.com")
ADMIN_PASSWORD = os.environ.get("SHOPFLOW_ADMIN_PASSWORD")
if not ADMIN_PASSWORD:
    sys.exit("SHOPFLOW_ADMIN_PASSWORD is not set — add it to the MCP server's env config")

_token: str | None = None
_token_expiry: float = 0


def get_token() -> str:
    global _token, _token_expiry
    if _token and time.time() < _token_expiry:
        return _token
    r = requests.post(f"{SHOPFLOW_API}/auth/login", json={
        "email": ADMIN_EMAIL,
        "password": ADMIN_PASSWORD
    }, timeout=90)
    r.raise_for_status()
    data = r.json()
    _token = data["token"]
    _token_expiry = time.time() + 12 * 3600  # tokens last 24h on the microservices, 7d on the monolith; refresh well before either
    return _token


def headers() -> dict:
    return {"Authorization": f"Bearer {get_token()}"}


def fetch_all(path: str, params: dict | None = None, page_size: int = 100, max_pages: int = 200) -> list:
    """Walk every page of a Spring Page<T> endpoint and return the combined content."""
    items: list = []
    for page in range(max_pages):
        r = requests.get(f"{SHOPFLOW_API}{path}", headers=headers(),
                         params={**(params or {}), "page": page, "size": page_size}, timeout=90)
        r.raise_for_status()
        body = r.json()
        items.extend(body.get("content", []))
        if body.get("last", True) or not body.get("content"):
            break
    return items


server = Server("shopflow-mcp")


@server.list_tools()
async def list_tools() -> list[Tool]:
    return [
        Tool(
            name="get_orders",
            description="Get all orders from ShopFlow store",
            inputSchema={
                "type": "object",
                "properties": {
                    "status": {
                        "type": "string",
                        "enum": ["PENDING", "CONFIRMED", "CANCELLED"],
                        "description": "Filter by order status (optional)"
                    }
                }
            }
        ),
        Tool(
            name="get_products",
            description="Get all products from ShopFlow store",
            inputSchema={
                "type": "object",
                "properties": {
                    "q": {"type": "string", "description": "Search query (optional)"}
                }
            }
        ),
        Tool(
            name="get_low_stock_products",
            description="Get products with stock below a threshold",
            inputSchema={
                "type": "object",
                "properties": {
                    "threshold": {"type": "integer", "description": "Stock threshold (default 10)"}
                }
            }
        ),
        Tool(
            name="update_inventory",
            description="Update stock quantity for a product",
            inputSchema={
                "type": "object",
                "properties": {
                    "product_id": {"type": "string"},
                    "quantity": {"type": "integer"}
                },
                "required": ["product_id", "quantity"]
            }
        ),
        Tool(
            name="update_order_status",
            description="Update the status of an order",
            inputSchema={
                "type": "object",
                "properties": {
                    "order_id": {"type": "string"},
                    "status": {
                        "type": "string",
                        "enum": ["CONFIRMED", "CANCELLED"]
                    }
                },
                "required": ["order_id", "status"]
            }
        ),
        Tool(
            name="search_orders_by_email",
            description="Find all orders for a specific customer email",
            inputSchema={
                "type": "object",
                "properties": {
                    "email": {"type": "string"}
                },
                "required": ["email"]
            }
        ),
        Tool(
            name="get_dashboard_stats",
            description="Get store statistics - total orders, revenue, users",
            inputSchema={"type": "object", "properties": {}}
        )
    ]


@server.call_tool()
async def call_tool(name: str, arguments: dict) -> list[TextContent]:
    h = headers()

    if name == "get_orders":
        params = {}
        if "status" in arguments:
            params["status"] = arguments["status"]
        r = requests.get(f"{SHOPFLOW_API}/admin/orders?size=100", headers=h, params=params, timeout=90)
        return [TextContent(type="text", text=r.text)]

    if name == "get_products":
        params = {"q": arguments.get("q", ""), "size": 100}
        r = requests.get(f"{SHOPFLOW_API}/products", params=params, timeout=90)
        return [TextContent(type="text", text=r.text)]

    if name == "get_low_stock_products":
        threshold = arguments.get("threshold", 10)
        try:
            items = fetch_all("/inventory")
        except requests.HTTPError as e:
            return [TextContent(type="text", text=f"Could not load inventory: {e}")]
        low = [i for i in items if i.get("availableQuantity", 0) < threshold]
        low.sort(key=lambda i: i.get("availableQuantity", 0))
        return [TextContent(type="text", text=str(low))]

    if name == "update_inventory":
        r = requests.put(
            f"{SHOPFLOW_API}/inventory/{arguments['product_id']}",
            headers=h,
            json={"quantity": arguments["quantity"]},
            timeout=90
        )
        return [TextContent(type="text", text=r.text)]

    if name == "update_order_status":
        r = requests.patch(
            f"{SHOPFLOW_API}/admin/orders/{arguments['order_id']}/status",
            headers=h,
            json={"status": arguments["status"]},
            timeout=90
        )
        return [TextContent(type="text", text=r.text)]

    if name == "search_orders_by_email":
        email = arguments["email"].strip().lower()
        try:
            orders = fetch_all("/admin/orders")
        except requests.HTTPError as e:
            return [TextContent(type="text", text=f"Could not load orders: {e}")]
        matches = [o for o in orders if o.get("userEmail", "").lower() == email]
        return [TextContent(type="text", text=str(matches))]

    if name == "get_dashboard_stats":
        try:
            orders = fetch_all("/admin/orders")
            users = requests.get(f"{SHOPFLOW_API}/admin/users", headers=h, params={"size": 1}, timeout=90)
            users.raise_for_status()
        except requests.HTTPError as e:
            return [TextContent(type="text", text=f"Could not load stats: {e}")]
        counts = {"CONFIRMED": 0, "PENDING": 0, "CANCELLED": 0}
        revenue = 0.0
        for o in orders:
            counts[o.get("status")] = counts.get(o.get("status"), 0) + 1
            if o.get("status") != "CANCELLED":
                revenue += float(o.get("totalAmount") or 0)
        stats = {
            "total_orders": len(orders),
            "confirmed": counts["CONFIRMED"],
            "pending": counts["PENDING"],
            "cancelled": counts["CANCELLED"],
            "total_revenue": f"${revenue:.2f}",
            "total_users": users.json().get("totalElements"),
        }
        return [TextContent(type="text", text=str(stats))]

    return [TextContent(type="text", text=f"Unknown tool: {name}")]


async def main():
    async with stdio_server() as (read, write):
        await server.run(read, write, server.create_initialization_options())


if __name__ == "__main__":
    asyncio.run(main())
