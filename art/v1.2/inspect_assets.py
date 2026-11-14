import bpy,json
from pathlib import Path
root=(Path(__file__).resolve().parents[2] / 'android/app/src/main/assets/mindsoul/WishDragon')
result={}
for name in ['wish-balls.glb','wish-dragon-motion.glb','wish-dragon-mindsoul.glb']:
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(root/name))
 result[name]=[{'name':o.name,'type':o.type,'verts':len(o.data.vertices) if o.type=='MESH' else 0,'location':list(o.location),'dimensions':list(o.dimensions),'materials':[m.name if m else None for m in o.data.materials] if o.type=='MESH' else []} for o in bpy.context.scene.objects]
print('ASSET_REPORT '+json.dumps(result))
(Path(__file__).resolve().parents[2] / 'art/v1.2/asset-inspection.json').write_text(json.dumps(result,indent=2))
