import os, math, colorsys, numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageEnhance
SRC="/mnt/user-data/outputs/cartography-art/src/"; ART="/mnt/user-data/outputs/cartography-art/"
def load(name):
    s=open(SRC+name).read(); s=s[:s.index("\nSET")]
    cwd=os.getcwd(); os.chdir(SRC); ns={}; exec(s,ns); os.chdir(cwd); return ns
A=load("actions.py"); D=load("dims.py")
GREEN=(57,255,140); DARK=(14,18,22); W=(232,240,236); MUTE=(130,142,150); CARD=(22,28,34)
WD,HT=1280,720
F=lambda s,b=False: ImageFont.truetype("/usr/share/fonts/opentype/inter/Inter-%s.otf"%("SemiBold" if b else "Medium"),s)
MONO=lambda s: ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",s)
PICK=(255,154,46); PHEX="#FF9A2E"

def recolor(im,rgb):
    a=np.asarray(im.convert("RGBA")).astype(float)/255; out=a.copy()
    th,ts,tv=colorsys.rgb_to_hsv(*[c/255 for c in rgb])
    for y in range(a.shape[0]):
        for x in range(a.shape[1]):
            r,g,b,al=a[y,x]
            if al==0: continue
            h,s,v=colorsys.rgb_to_hsv(r,g,b)
            if s>0.35: out[y,x,:3]=colorsys.hsv_to_rgb(th,max(s,ts*0.9),v*max(tv,0.5)/1.0 if v>0 else v)
    return Image.fromarray((out*255).astype(np.uint8))

# background: the world map, dimmed, with the new mark placed at the cursor
bg=Image.open(ART+"ui/ui_world_map.png").convert("RGBA")
bg=ImageEnhance.Brightness(bg).enhance(0.7); img=bg
d=ImageDraw.Draw(img)
DX=840
mine=recolor(Image.open(ART+"marks/mark_mine_32.png"),PICK)
img.alpha_composite(mine,(700-16,560-16))
d.text((700-d.textlength("Copper Vein",font=F(13,True))/2,560-34),"Copper Vein",font=F(13,True),fill=W,stroke_width=3,stroke_fill=(10,14,18))
def brackets(x0,y0,x1,y1,b=12,w=2,c=GREEN):
    for x,y,sx,sy in [(x0,y0,1,1),(x1,y0,-1,1),(x0,y1,1,-1),(x1,y1,-1,-1)]:
        d.line([(x,y),(x+sx*b,y)],fill=c,width=w); d.line([(x,y),(x,y+sy*b)],fill=c,width=w)
# pulse ring around the placed mark
for r,a in [(26,120),(34,60)]: d.ellipse([700-r,560-r,700+r,560+r],outline=PICK+(a,),width=2)

# drawer
d.rectangle([DX,0,WD,HT],fill=(12,16,20)); d.line([(DX,0),(DX,HT)],fill=GREEN,width=2)
g=Image.new("RGBA",(WD,HT),(0,0,0,0)); ImageDraw.Draw(g).rectangle([DX-6,0,DX+2,HT],fill=(57,255,140,60))
img.alpha_composite(g.filter(ImageFilter.GaussianBlur(5))); d=ImageDraw.Draw(img)
px=DX+24; pr=WD-24
def label(y,t): d.text((px,y),t.upper(),font=F(10,True),fill=MUTE)
# header
img.alpha_composite(A["act_chip"](32,A["newmark"]),(px,18))
d.text((px+44,18),"NEW MARK",font=F(15,True),fill=GREEN); d.text((px+44,38),"Overworld  ·  960, 12, -240",font=F(11),fill=MUTE)
# name
y=70; label(y,"Name")
d.rectangle([px,y+16,pr,y+46],fill=CARD,outline=GREEN); d.text((px+10,y+22),"Copper Vein",font=F(14,True),fill=W)
cx=px+10+d.textlength("Copper Vein",font=F(14,True))+2; d.line([(cx,y+21),(cx,y+41)],fill=GREEN,width=2)
# emblem grid
y=130; label(y,"Emblem")
names=["keep","home","village","farm","mine","treasure","shrine","ruin","skull","gate","flag","star","question","danger"]
cell=(pr-px)//7
for i,n in enumerate(names):
    ex=px+(i%7)*cell; ey=y+18+(i//7)*(cell+4)
    sel=(n=="mine")
    d.rectangle([ex+2,ey,ex+cell-2,ey+cell-4],fill=(30,26,18) if sel else CARD,outline=PICK if sel else (40,50,56))
    ic=Image.open(ART+f"marks/mark_{n}_64.png").convert("RGBA")
    if sel: ic=recolor(ic,PICK)
    ic=ic.resize((cell-14,cell-14),Image.LANCZOS); img.alpha_composite(ic,(ex+7,ey+5))
    if sel: brackets(ex,ey-2,ex+cell,ey+cell-2,8,2,PICK)
# hover tag on grid
def tag(x,y,text):
    f=F(12,True); tw=d.textlength(text,font=f)
    d.rectangle([x,y,x+tw+20,y+24],fill=DARK); brackets(x,y,x+tw+20,y+24,6,1); d.text((x+10,y+4),text,font=f,fill=W)
# color wheel
y=262; label(y,"Color")
R=78; wcx,wcy=px+R+2,y+20+R
ring=Image.new("RGBA",(2*R*4,2*R*4),(0,0,0,0)); rd=ImageDraw.Draw(ring)
for a in range(360):
    c=tuple(int(v*255) for v in colorsys.hsv_to_rgb(a/360,1,1))
    rd.pieslice([0,0,8*R,8*R],a-90,a-89+1.5,fill=c)
rd.ellipse([4*14,4*14,8*R-4*14,8*R-4*14],fill=(0,0,0,0))
mask=Image.new("L",ring.size,0); md=ImageDraw.Draw(mask); md.ellipse([0,0,8*R,8*R],fill=255); md.ellipse([4*14,4*14,8*R-4*14,8*R-4*14],fill=0)
ring.putalpha(mask); ring=ring.resize((2*R,2*R),Image.LANCZOS); img.alpha_composite(ring,(wcx-R,wcy-R)); d=ImageDraw.Draw(img)
# shade square
S=int((R-14)*1.35); sx0,sy0=wcx-S//2,wcy-S//2
h,_,_=colorsys.rgb_to_hsv(*[c/255 for c in PICK])
sq=np.zeros((S,S,3))
for yy in range(S):
    for xx in range(S): sq[yy,xx]=colorsys.hsv_to_rgb(h,xx/(S-1),1-yy/(S-1))
img.paste(Image.fromarray((sq*255).astype(np.uint8)),(sx0,sy0)); d=ImageDraw.Draw(img)
_,ss,vv=colorsys.rgb_to_hsv(*[c/255 for c in PICK])
kx,ky=sx0+ss*(S-1),sy0+(1-vv)*(S-1); d.ellipse([kx-6,ky-6,kx+6,ky+6],outline=W,width=2); d.ellipse([kx-8,ky-8,kx+8,ky+8],outline=DARK,width=1)
ang=math.radians(h*360-90); hr=R-7; hx,hy=wcx+hr*math.cos(ang),wcy+hr*math.sin(ang)
d.ellipse([hx-8,hy-8,hx+8,hy+8],outline=W,width=3)
# right of wheel: hex, quick colors, recent
rx=px+2*R+20
d.text((rx,y+20),"HEX",font=F(10,True),fill=MUTE)
d.rectangle([rx,y+34,rx+110,y+60],fill=CARD,outline=(40,50,56)); d.text((rx+8,y+39),PHEX,font=MONO(13),fill=W)
d.rectangle([rx+118,y+34,rx+144,y+60],fill=PICK,outline=W)
d.text((rx,y+70),"QUICK",font=F(10,True),fill=MUTE)
quick=[(57,255,140),(40,200,90),(120,220,110),(64,224,208),(70,170,255),(60,90,230),(150,100,255),(200,120,255),
       (255,110,200),(255,80,80),(255,154,46),(255,210,60),(240,240,240),(160,166,172),(140,100,60),(90,60,40)]
sw=18
for i,c in enumerate(quick):
    qx=rx+(i%8)*(sw+4); qy=y+84+(i//8)*(sw+4)
    d.rectangle([qx,qy,qx+sw,qy+sw],fill=c,outline=W if c==PICK else (30,36,42),width=2 if c==PICK else 1)
d.text((rx,y+136),"RECENT",font=F(10,True),fill=MUTE)
recent=[(255,154,46),(57,255,140),(70,170,255),(255,80,80),(200,120,255),(64,224,208),(255,210,60),(240,240,240)]
for i,c in enumerate(recent):
    qx=rx+i*(sw+4); qy=y+150; d.rectangle([qx,qy,qx+sw,qy+sw],fill=c,outline=(30,36,42))
# folio + preview
y=452; label(y,"Folio")
d.rectangle([px,y+16,px+180,y+44],fill=CARD,outline=(40,50,56)); d.text((px+10,y+21),"Mining",font=F(13,True),fill=W)
d.polygon([(px+162,y+27),(px+170,y+27),(px+166,y+32)],fill=MUTE)
d.text((px+200,y),"OPTIONS",font=F(10,True),fill=MUTE)
def toggle(x,yy,t,on):
    d.rounded_rectangle([x,yy,x+30,yy+16],radius=8,fill=(30,90,60) if on else (40,48,54)); d.ellipse([x+(16 if on else 2),yy+2,x+(28 if on else 14),yy+14],fill=GREEN if on else MUTE)
    d.text((x+38,yy),t,font=F(12),fill=W)
toggle(px+200,y+18,"In the world",True); toggle(px+200,y+40,"On minimap",True)
# in-world preview
y=520; label(y,"Preview in the world")
pv=(px,y+16,pr,y+136)
sky=Image.new("RGBA",(pv[2]-pv[0],pv[3]-pv[1]))
sa=np.zeros((sky.height,sky.width,4),dtype=np.uint8)
for yy in range(sky.height):
    t=yy/sky.height; sa[yy,:,:3]=(np.array([18,26,40])*(1-t)+np.array([30,40,48])*t); sa[yy,:,3]=255
sky=Image.fromarray(sa); sd=ImageDraw.Draw(sky)
# ground blocks
for i in range(0,sky.width,16):
    hgt=88+int(6*math.sin(i*0.07))
    sd.rectangle([i,hgt,i+15,sky.height],fill=(52,74,58)); sd.rectangle([i,hgt,i+15,hgt+3],fill=(70,98,74))
img.paste(sky,(pv[0],pv[1])); d=ImageDraw.Draw(img)
mx,my=(pv[0]+pv[2])//2,pv[1]+52
_g=Image.open(ART+"textures/mark_glow_64.png").convert("RGBA"); glow=Image.new("RGBA",_g.size,PICK+(0,)); glow.putalpha(_g.getchannel("A")); glow=glow.resize((96,96),Image.LANCZOS)
img.alpha_composite(glow,(mx-48,my-48))
_d=Image.open(ART+"textures/mark_drip_16x48.png").convert("RGBA"); drip=Image.new("RGBA",_d.size,PICK+(0,)); drip.putalpha(_d.getchannel("A")); img.alpha_composite(drip,(mx-8,my+14))
big=recolor(Image.open(ART+"marks/mark_mine_64.png"),PICK).resize((40,40),Image.LANCZOS); img.alpha_composite(big,(mx-20,my-20))
d=ImageDraw.Draw(img)
d.text((mx-d.textlength("Copper Vein",font=F(12,True))/2,my-38),"Copper Vein",font=F(12,True),fill=W,stroke_width=2,stroke_fill=(10,14,18))
d.text((mx-d.textlength("274m",font=MONO(11))/2,my+24),"274m",font=MONO(11),fill=(200,210,205),stroke_width=2,stroke_fill=(10,14,18))
d.rectangle(pv,outline=(40,50,56))
# actions
ay=HT-62
d.line([(px,ay-12),(pr,ay-12)],fill=(40,50,56))
img.alpha_composite(A["act_chip"](40,A["confirm"]),(px,ay))
d.text((px+52,ay+4),"Save",font=F(13,True),fill=GREEN); d.text((px+52,ay+22),"Enter",font=F(11),fill=MUTE)
d.text((pr-d.textlength("Esc to cancel",font=F(12)),ay+12),"Esc to cancel",font=F(12),fill=MUTE)
# hover on the hue ring and grid
tag(px+cell*2-10,130+18+cell+cell-2,"Gate")
img.convert("RGB").save("ui_mark_editor.png"); print("ok")
