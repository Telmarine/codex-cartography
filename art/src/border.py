import os
SRC="/mnt/user-data/outputs/cartography-art/src/"
s=open(SRC+"glyphs.py").read(); s=s[:s.index("\nSET")]
os.chdir(SRC); exec(s); os.chdir("/tmp/claude-0/-home-claude/4d923c7a-859c-52f2-826d-95709660c6ee/scratchpad/art")
def border(d,c,u,f):
    # the border wall: a solid line with the outside hatched, and an arrow on the inside pointing at it
    x=c+1.6*u
    d.line([(x,c-7.4*u),(x,c+7.4*u)],fill=f,width=int(1.6*u))
    for i in range(4):
        y=c-5.6*u+i*3.6*u
        d.line([(x+1.0*u,y+2.4*u),(x+5.0*u,y-1.0*u)],fill=f,width=int(1.1*u))
    d.line([(c-7.0*u,c),(c-2.6*u,c)],fill=f,width=int(1.4*u))
    d.polygon([(c-0.6*u,c),(c-3.2*u,c-2.8*u),(c-3.2*u,c+2.8*u)],fill=f)
on=chip(24,border,True); off=chip(24,border,False)
on.save("layer_border_on_24.png"); off.save("layer_border_off_24.png")
from PIL import Image
sh=Image.new("RGBA",(340,140),(8,10,12,255))
sh.alpha_composite(on.resize((96,96),Image.NEAREST),(20,20)); sh.alpha_composite(off.resize((96,96),Image.NEAREST),(130,20))
sh.alpha_composite(on,(250,40)); sh.alpha_composite(off,(290,40))
sh.convert("RGB").save("border_preview.png")
