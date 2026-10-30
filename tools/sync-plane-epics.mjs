import fs from 'fs';
import path from 'path';

const API_KEY = "plane_api_8625faf21c2e48f8826328d400838ac1";
const BASE_URL = "http://localhost:8082/api/v1/workspaces/life-path/projects/8e0eabe0-d3a8-4429-ad73-f156c7b6ab1f";

const LABELS = {
    "Feature": "ce807d8d-1d36-432e-b023-514098034de2",
    "Game Designer": "86717885-331a-44c9-a686-60e71669ad28",
    "Software Engineer": "b1dae36e-dac9-4f53-8158-9d9ce2df7f9a",
    "DevOps": "7134143f-296d-41ba-a572-f1bf73fc777b",
    "Bug": "05726d3d-aaed-4540-98a9-c34d287e5d4a"
};

async function createWorkItem(payload, retries = 3) {
    for (let i = 0; i < retries; i++) {
        const res = await fetch(`${BASE_URL}/work-items/`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-API-Key': API_KEY,
            },
            body: JSON.stringify(payload)
        });
        if (res.status === 429) {
            console.log("Rate limit hit, waiting 5 seconds...");
            await new Promise(r => setTimeout(r, 5000));
            continue;
        }
        if (!res.ok) {
            const text = await res.text();
            console.error("Failed to create work item:", payload.name, res.status, text);
            throw new Error(`Failed to create ${payload.name}`);
        }
        return res.json();
    }
    throw new Error(`Failed to create ${payload.name} after ${retries} retries`);
}

async function run() {
    const md = fs.readFileSync(path.join(process.cwd(), 'docs/SCRUM_BOARD.md'), 'utf-8');

    const epics = [];
    const epicBlocks = md.split('### EPIC');
    epicBlocks.shift(); // remove intro

    for (const block of epicBlocks) {
        const lines = block.split('\n');
        const titleLine = lines[0].trim();
        // e.g. " 1 · `[Feature] Citizen A* Pathfinding` — P0-1 🔴"
        const match = titleLine.match(/(\d+) · `([^`]+)` — (P\d+-[A-Z0-9]+)/);
        if (!match) continue;

        const epicNum = match[1];
        const epicTitle = match[2];
        const epicPriority = match[3];

        // Description is the quote right after
        const descMatch = block.match(/> ([^\n]+)/);
        const description = descMatch ? descMatch[1] : "";

        const roles = {
            GD: [],
            SE: [],
            DevOps: [],
            Test: []
        };

        // Parse tables
        const tableRegex = /\|\s*([\w\.]+)\s*\|\s*([^\|]+)\s*\|\s*([^\|]+)\s*\|[^\|]+\|[^\|]+\|/g;
        let tableMatch;
        while ((tableMatch = tableRegex.exec(block)) !== null) {
            const id = tableMatch[1].trim();
            if (id === '#' || id === 'Test') continue;

            const task = tableMatch[2].trim();
            const roleStr = tableMatch[3].trim();

            if (id.includes('T')) {
                roles.Test.push(task);
            } else {
                if (roleStr.includes('GD') || roleStr.includes('Designer') || roleStr.includes('Design')) {
                    roles.GD.push(task);
                }
                if (roleStr.includes('SWE') || roleStr.includes('Physics') || roleStr.includes('Engineer')) {
                    roles.SE.push(task);
                }
                if (roleStr.includes('PM')) {
                    // Include PM tasks in GD or SE
                    roles.GD.push(`[PM API] ${task}`);
                }
            }
        }

        epics.push({
            title: `${epicTitle} (${epicPriority})`,
            description,
            roles
        });
    }

    console.log(`Found ${epics.length} EPICs to sync.`);

    for (const epic of epics) {
        console.log(`Creating EPIC: ${epic.title}`);
        const parentPayload = {
            name: epic.title,
            description_html: `<h3>Context</h3><p>${epic.description}</p>`,
            labels: [LABELS["Feature"]]
        };

        const parent = await createWorkItem(parentPayload);
        const parentId = parent.id;

        if (epic.roles.GD.length > 0) {
            await createWorkItem({
                name: `[GD] ${epic.title.replace('[Feature]', '').trim()}`,
                parent: parentId,
                labels: [LABELS["Game Designer"]],
                description_html: `<h3>Tasks</h3><ul>${epic.roles.GD.map(t => `<li>${t}</li>`).join('')}</ul>`
            });
        }

        if (epic.roles.SE.length > 0) {
            await createWorkItem({
                name: `[SE] ${epic.title.replace('[Feature]', '').trim()}`,
                parent: parentId,
                labels: [LABELS["Software Engineer"]],
                description_html: `<h3>Tasks</h3><ul>${epic.roles.SE.map(t => `<li>${t}</li>`).join('')}</ul>`
            });
        }

        if (epic.roles.Test.length > 0) {
            await createWorkItem({
                name: `[DevOps] Testing: ${epic.title.replace('[Feature]', '').trim()}`,
                parent: parentId,
                labels: [LABELS["DevOps"]], // using DevOps for QA/Testing per instructions
                description_html: `<h3>Test Cases</h3><ul>${epic.roles.Test.map(t => `<li>${t}</li>`).join('')}</ul>`
            });
        }

        // Sleep slightly to avoid rate limit
        await new Promise(r => setTimeout(r, 100));
    }

    console.log("Finished syncing all tickets!");
}

run().catch(console.error);
