// SPDX-License-Identifier: AGPL-3.0-only
import { createRelayServer } from "./server.js";

const masterKeyHex = process.env.REPROPARCEL_MASTER_KEY ?? "";
if (!/^[a-f0-9]{64}$/i.test(masterKeyHex)) {
  throw new Error("REPROPARCEL_MASTER_KEY must be 64 hex characters");
}
const adminToken = process.env.REPROPARCEL_ADMIN_TOKEN;
if (!adminToken || adminToken.length < 32) {
  throw new Error("REPROPARCEL_ADMIN_TOKEN must be at least 32 characters");
}
const port = Number(process.env.REPROPARCEL_PORT ?? "8080");
const dbPath = process.env.REPROPARCEL_DB ?? "reproparcel.sqlite";

const { server, store } = createRelayServer({
  dbPath,
  masterKey: Buffer.from(masterKeyHex, "hex"),
  adminToken,
});

if (process.env.REPROPARCEL_PROJECT_ID && process.env.REPROPARCEL_PROJECT_TOKEN) {
  store.provisionProject(process.env.REPROPARCEL_PROJECT_ID, process.env.REPROPARCEL_PROJECT_TOKEN);
}

server.listen(port, "127.0.0.1", () => {
  process.stdout.write(`reproparcel relay listening on 127.0.0.1:${port}\n`);
});
