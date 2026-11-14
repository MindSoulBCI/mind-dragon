import bpy
from mathutils import Vector
from pathlib import Path
root=(Path(__file__).resolve().parents[2] / 'art/v1.2')
bpy.ops.wm.open_mainfile(filepath=str(root/'ball-7.blend'))
s=bpy.context.scene
bpy.ops.object.camera_add(location=(0,-1,0))
c=bpy.context.object;c.rotation_euler=(Vector((0,0,0))-c.location).to_track_quat('-Z','Y').to_euler();c.data.type='ORTHO';c.data.ortho_scale=.42;s.camera=c
bpy.ops.object.light_add(type='AREA',location=(-.3,-.5,.5));bpy.context.object.data.energy=25;bpy.context.object.data.shape='DISK';bpy.context.object.data.size=1
s.world=bpy.data.worlds.new('PreviewWorld');s.world.use_nodes=True;s.world.node_tree.nodes['Background'].inputs[0].default_value=(.15,.18,.22,1)
s.render.engine='CYCLES';s.cycles.samples=16;s.render.resolution_x=600;s.render.resolution_y=600;s.render.resolution_percentage=100;s.render.filepath=str(root/'seven-logos-preview.png');bpy.ops.render.render(write_still=True)
