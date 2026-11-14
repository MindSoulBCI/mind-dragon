import bpy,math,json
from pathlib import Path
from mathutils import Vector,Matrix
ROOT=(Path(__file__).resolve().parents[2] / '')
ASSETS=ROOT/'android/app/src/main/assets/mindsoul/WishDragon'
OUT=ASSETS/'interactive';OUT.mkdir(exist_ok=True)
ART=ROOT/'art/v1.2'
def material(name,color,metal=0,rough=.35,alpha=1,emission=0):
 m=bpy.data.materials.new(name);m.use_nodes=True;p=m.node_tree.nodes.get('Principled BSDF');p.inputs['Base Color'].default_value=(*color,alpha);p.inputs['Metallic'].default_value=metal;p.inputs['Roughness'].default_value=rough;p.inputs['Alpha'].default_value=alpha;p.inputs['Emission Color'].default_value=(*color,1);p.inputs['Emission Strength'].default_value=emission
 if alpha<1:m.surface_render_method='DITHERED';m.use_transparency_overlap=False
 m.diffuse_color=(*color,alpha);return m
def bake(o):
 matrix=o.matrix_world.copy();o.parent=None;o.matrix_world=Matrix.Identity(4);o.data.transform(matrix)
def export(path):bpy.ops.export_scene.gltf(filepath=str(path),export_format='GLB',use_selection=True,export_animations=False,export_yup=True)
# Preserve original authored stars; normalize each sphere independently.
report=[]
for number,prefix in enumerate(['One_Star','Two_Star','Star_Three','Four_Star','Five_Star','Six_Star','Seven_Star'],1):
 bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(ASSETS/'wish-balls.glb'))
 shell=next(o for o in bpy.context.scene.objects if o.type=='MESH' and o.name.startswith(prefix+'_02'))
 star=next(o for o in bpy.context.scene.objects if o.type=='MESH' and o.name.startswith(prefix+'_03'))
 bake(shell);bake(star)
 points=[v.co for v in shell.data.vertices];lo=Vector(tuple(min(v[i] for v in points) for i in range(3)));hi=Vector(tuple(max(v[i] for v in points) for i in range(3)));center=(lo+hi)*.5;factor=.32/max(hi-lo)
 for o in [shell,star]:
  for v in o.data.vertices:v.co=(v.co-center)*factor
  o.data.update()
 shell.name='BallShell'
 shell.data.materials.clear();shell.data.materials.append(material('AmberGlass',(1.0,.25,.015),.08,.26,.82,.30))
 # One red circular mark per star, with a separate gold emission variant.
 idle_mat=material('StarDotRed',(.65,.025,.008),0,.4,1,.3)
 glow_mat=material('StarDotGold',(1,.68,.12),0,.4,1,6)
 patterns={1:[(0,0)],2:[(-.052,0),(.052,0)],3:[(0,.048),(-.05,-.04),(.05,-.04)],4:[(-.048,.048),(.048,.048),(-.048,-.048),(.048,-.048)],5:[(-.06,.06),(.06,.06),(0,0),(-.06,-.06),(.06,-.06)],6:[(-.065,.045),(0,.045),(.065,.045),(-.065,-.045),(0,-.045),(.065,-.045)],7:[(0,0)]+[(math.cos(i*math.tau/6)*.083,math.sin(i*math.tau/6)*.083) for i in range(6)]}
 radius=.022
 verts=[];faces=[]
 for x,z in patterns[number]:
  k=len(verts);verts.append((x,-.168,z))
  for i in range(32):
   angle=i*math.tau/32;verts.append((x+math.cos(angle)*radius,-.168,z+math.sin(angle)*radius))
  for i in range(32):faces.append((k,k+1+i,k+1+(i+1)%32))
 mesh=bpy.data.meshes.new('StarDotDiscs');mesh.from_pydata(verts,[],faces)
 idle=bpy.data.objects.new('LogosIdle',mesh);bpy.context.collection.objects.link(idle);idle.data.materials.append(idle_mat)
 glow=idle.copy();glow.data=idle.data.copy();bpy.context.collection.objects.link(glow);glow.name='LogosGlow';glow.data.materials.clear();glow.data.materials.append(glow_mat)
 halo=shell.copy();halo.data=shell.data.copy();bpy.context.collection.objects.link(halo);halo.name='BallGlow';halo.scale=(1.055,)*3;halo.data.materials.clear();halo.data.materials.append(material('HaloGold',(1,.58,.055),0,.3,.12,2))
 keep={shell,idle,glow,halo}
 for o in list(bpy.context.scene.objects):
  if o not in keep:bpy.data.objects.remove(o,do_unlink=True)
 bpy.ops.object.select_all(action='SELECT');export(OUT/f'ball-{number}.glb')
 glow.hide_render=True;halo.hide_render=True
 bpy.ops.wm.save_as_mainfile(filepath=str(ART/f'ball-{number}.blend'))
 report.append({'ball':number,'shellDiameter':.32,'dotCount':number,'idle':'LogosIdle','hover':'LogosGlow'})
# Blue dragon, keeping sculpted whisker and jaw pivots from existing motion mesh.
bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(ASSETS/'wish-dragon-motion.glb'))
blue=material('AzureDragonScales',(.018,.22,.72),.25,.32,1,.12)
belly=material('PearlBlueBelly',(.38,.72,.92),.12,.42)
for o in bpy.context.scene.objects:
 if o.type!='MESH':continue
 if o.name.startswith('Shenron_Green'):
  o.data.materials.clear();o.data.materials.append(blue)
 if o.name.startswith('Shenron_Yellow'):
  o.data.materials.clear();o.data.materials.append(belly)
renames={'Shenron_Green_0_001':'WhiskerLeft','Shenron_Green_0_002':'WhiskerRight','Shenron_Yellow_0_001':'LowerJaw','Shenron_Green_0':'DragonBody'}
for old,new in renames.items():bpy.data.objects[old].name=new
# Editable slow preview animation; runtime evaluates the same local pivot rotations.
bpy.context.scene.render.fps=30;bpy.context.scene.frame_end=240
for name,axis,amplitude,phase in [('WhiskerLeft',1,.10,0),('WhiskerRight',1,-.09,.8),('LowerJaw',0,.045,0)]:
 o=bpy.data.objects[name];base=o.rotation_euler.copy()
 for frame in range(1,242,6):
  t=(frame-1)/30;o.rotation_euler=base.copy();o.rotation_euler[axis]+=amplitude*(.5+.5*math.sin(t*.8)) if name=='LowerJaw' else amplitude*math.sin(t*.8+phase);o.keyframe_insert(data_path='rotation_euler',frame=frame)
 o.rotation_euler=base
bpy.context.scene.frame_set(1)
# Reset authored rest rotations for export, animation remains in .blend as editing reference.
for name in ['WhiskerLeft','WhiskerRight','LowerJaw']:bpy.data.objects[name].rotation_euler=(0,0,0)
bpy.ops.object.select_all(action='SELECT');export(OUT/'dragon-azure.glb')
bpy.ops.wm.save_as_mainfile(filepath=str(ART/'dragon-azure.blend'))
report.append({'dragon':'dragon-azure.glb','motionNodes':['WhiskerLeft','WhiskerRight','LowerJaw'],'color':'azure blue','runtimeMotion':'local pivot quaternion offsets, 8 second gentle cycle'})
(ART/'asset-manifest.json').write_text(json.dumps(report,ensure_ascii=False,indent=2))
print('DONE',OUT)
