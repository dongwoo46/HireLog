import path from "node:path";
import { fileURLToPath } from "node:url";
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";

async function main() {
  const __filename = fileURLToPath(import.meta.url);
  const __dirname = path.dirname(__filename);
  const serverEntry = path.resolve(__dirname, "../dist/index.js");

  const transport = new StdioClientTransport({
    command: "node",
    args: [serverEntry],
    env: {
      ...process.env
    }
  });

  const client = new Client(
    { name: "hirelog-mcp-test-client", version: "0.1.0" },
    { capabilities: {} }
  );

  await client.connect(transport);

  const tools = await client.listTools();
  console.log("TOOLS:");
  console.log(tools.tools.map((t) => t.name).join(", "));

  const ping = await client.callTool({
    name: "ping",
    arguments: {}
  });
  console.log("\nPING:");
  console.log(JSON.stringify(ping, null, 2));

  const jdList = await client.callTool({
    name: "jd_list",
    arguments: {
      keyword: "backend",
      size: 3
    }
  });
  console.log("\nJD_LIST:");
  console.log(JSON.stringify(jdList, null, 2));

  await client.close();
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
