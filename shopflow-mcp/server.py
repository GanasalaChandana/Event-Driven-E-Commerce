import requests
import asyncio
import os
import time
from mcp.server import Server
from mcp.server.stdio import stdio_server
from mcp.types import Tool, TextContent

SHOPFLOW_API = "https://event-driven-e-commerce.onrender.com/api/v1"
ADMIN_EMAIL = os.environ.get("SHOPFLOW_ADMIN_EMAIL", "admin@shopflow.com")
ADMIN_PASSWORD = os.environ["SHOPFLOW_ADMIN_PASSWORD"]

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
    _token_expiry = time.time() + 6 * 24 * 3600  # refresh after 6 days
    return _token


def headers() -> dict:
    return {"Authorization": f"Bearer {get_token()}"}


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
        r = requests.get(f"{SHOPFLOW_API}/inventory", headers=h, timeout=90)
        if r.status_code != 200:
            return [TextContent(type="text", text=r.text)]
        items = r.json() if isinstance(r.json(), list) else r.json().get("content", [])
        low = [i for i in items if i.get("availableQuantity", 999) < threshold]
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
        r = requests.get(f"{SHOPFLOW_API}/admin/orders?size=100", headers=h, timeout=90)
        if r.status_code != 200:
            return [TextContent(type="text", text=r.text)]
        orders = r.json().get("content", [])
        email = arguments["email"].lower()
        matches = [o for o in orders if o.get("userEmail", "").lower() == email]
        return [TextContent(type="text", text=str(matches))]

    if name == "get_dashboard_stats":
        orders = requests.get(f"{SHOPFLOW_API}/admin/orders?size=100", headers=h, timeout=90).json()
        users = requests.get(f"{SHOPFLOW_API}/admin/users?size=1", headers=h, timeout=90).json()
        all_orders = orders.get("content", [])
        revenue = sum(o.get("totalAmount", 0) for o in all_orders if o.get("status") != "CANCELLED")
        confirmed = sum(1 for o in all_orders if o.get("status") == "CONFIRMED")
        pending = sum(1 for o in all_orders if o.get("status") == "PENDING")
        cancelled = sum(1 for o in all_orders if o.get("status") == "CANCELLED")
        stats = {
            "total_orders": orders.get("totalElements"),
            "confirmed": confirmed,
            "pending": pending,
            "cancelled": cancelled,
            "total_revenue": f"${revenue:.2f}",
            "total_users": users.get("totalElements")
        }
        return [TextContent(type="text", text=str(stats))]


async def main():
    async with stdio_server() as (read, write):
        await server.run(read, write, server.create_initialization_options())


if __name__ == "__main__":
    asyncio.run(main())
