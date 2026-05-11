import os

items = [
    "module_surface", "module_underground", "module_nether", "module_end",
    "speed_upgrade", "recycle_upgrade", "radar_upgrade", "parallel_upgrade",
    "upgrade_fluid", "upgrade_archeology", "upgrade_demolition_master",
    "upgrade_structure_interest", "upgrade_infinite", "upgrade_fortune",
    "upgrade_silk_touch", "target_pin"
]

base_path = "src/main/resources/assets/virtual_explorer/models/item"
os.makedirs(base_path, exist_ok=True)

for item in items:
    content = f'''{{
  "parent": "minecraft:item/generated",
  "textures": {{
    "layer0": "virtual_explorer:item/{item}"
  }}
}}'''
    with open(os.path.join(base_path, f"{item}.json"), "w") as f:
        f.write(content)

print(f"Generated {len(items)} model files.")
