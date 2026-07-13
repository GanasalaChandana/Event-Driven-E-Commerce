import requests
from mcp.server import Server
from mcp.server.stdio import stdio_server
from mcp.types import Tool, TextContent
import asyncio
import os

SHOPFLOW_API = "https://event-driven-e-commerce.onrender.com/api/v1"
ADMIN_TOKEN = os.environ["SHOPFLOW_ADMIN_TOKEN"]

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
            name="get_dashboard_stats",
            description="Get store statistics - total orders, revenue, users",
            inputSchema={"type": "object", "properties": {}}
        )
    ]


@server.call_tool()
async def call_tool(name: str, arguments: dict) -> list[TextContent]:
    headers = {"Authorization": f"Bearer {ADMIN_TOKEN}"}

    if name == "get_orders":
        params = {}
        if "status" in arguments:
            params["status"] = arguments["status"]
        r = requests.get(f"{SHOPFLOW_API}/admin/orders", headers=headers, params=params)
        return [TextContent(type="text", text=r.text)]

    if name == "get_products":
        params = {"q": arguments.get("q", "")}
        r = requests.get(f"{SHOPFLOW_API}/products", params=params)
        return [TextContent(type="text", text=r.text)]

    if name == "update_inventory":
        r = requests.put(
            f"{SHOPFLOW_API}/inventory/{arguments['product_id']}",
            headers=headers,
            json={"quantity": arguments["quantity"]}
        )
        return [TextContent(type="text", text=r.text)]

    if name == "update_order_status":
        r = requests.patch(
            f"{SHOPFLOW_API}/admin/orders/{arguments['order_id']}/status",
            headers=headers,
            json={"status": arguments["status"]}
        )
        return [TextContent(type="text", text=r.text)]

    if name == "get_dashboard_stats":
        orders = requests.get(f"{SHOPFLOW_API}/admin/orders?size=1", headers=headers).json()
        users = requests.get(f"{SHOPFLOW_API}/admin/users?size=1", headers=headers).json()
        return [TextContent(type="text", text=f"Total orders: {orders.get('totalElements')}, Total users: {users.get('totalElements')}")]


async def main():
    async with stdio_server() as (read, write):
        await server.run(read, write, server.create_initialization_options())


if __name__ == "__main__":
    asyncio.run(main())
