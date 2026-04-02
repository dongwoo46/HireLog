import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { createHirelogServer } from "./server.js";

async function main() {
  const server = createHirelogServer();
  const transport = new StdioServerTransport();
  await server.connect(transport);
}

main().catch((error) => {
  const message = error instanceof Error ? error.stack ?? error.message : String(error);
  console.error(message);
  process.exit(1);
});
