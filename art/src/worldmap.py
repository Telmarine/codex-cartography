import math, numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter
SRC="/mnt/user-data/outputs/cartography-art/src/"; ART="/mnt/user-data/outputs/cartography-art/"
import os
def load(name):
    s=open(SRC+name).read(); s=s[:s.index("\nSET")]
    cwd=os.getcwd(); os.chdir(SRC); ns={}; exec(s,ns); os.chdir(cwd); return ns
G=load("glyphs.py"); A=load("actions.py"); D=load("dims.py"); SM=load("servermarks.py")
GREEN=(57,255,140); DARK=(14,18,22); SLATE=(20,25,30); W=(232,240,236); MUTE=(130,142,150)
WD,HT=1280,720
F=lambda s,b=False: ImageFont.truetype("/usr/share/fonts/opentype/inter/Inter-%s.otf"%("SemiBold" if b else "Medium"),s)
MONO=lambda s: ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",s)

# ---- terrain at block scale (4px per block)
BS=4; bw,bh=WD//BS,HT//BS
rng=np.random.default_rng(7)
def noise(scale,seed):
    r=np.random.default_rng(seed); g=r.random((bh//scale+3,bw//scale+3))
    im=Image.fromarray((g*255).astype(np.uint8)).resize(((bw//scale+3)*scale,(bh//scale+3)*scale),Image.BICUBIC)
    return np.asarray(im,dtype=float)[:bh,:bw]/255
h=noise(40,1)*0.55+noise(16,2)*0.3+noise(6,3)*0.15
forest=noise(14,4); dry=noise(30,5)
col=np.zeros((bh,bw,3))
def put(mask,c): col[mask]=c
put(h>=0,(88,128,92))                                  # grass (cool, muted)
put((forest>0.55)&(h>0.47),(58,96,70))                 # forest
put((dry>0.62)&(h>0.47),(150,140,98))                  # dry plains
put(h>0.70,(112,116,118)); put(h>0.80,(196,204,210))   # stone, snow
put((h>0.43)&(h<0.47),(176,168,128))                   # shore
water=h<0.43
deep=np.clip((0.43-h)/0.15,0,1)
col[water]=(np.array([46,82,120])*(1-deep[water,None])+np.array([24,44,74])*deep[water,None])
# height shading (compare to block north)
land=~water; sh=np.zeros_like(h); sh[1:]=h[1:]-h[:-1]
f=np.clip(1+sh*14,0.78,1.18); col[land]*=f[land,None]
# crisp dark water edge
edge=np.zeros_like(water)
for dy,dx in [(1,0),(-1,0),(0,1),(0,-1)]: edge|=np.roll(np.roll(land,dy,0),dx,1)
col[water&edge]=(20,34,52)
terrain=Image.fromarray(np.clip(col,0,255).astype(np.uint8)).resize((WD,HT),Image.NEAREST)

# ---- explored mask + fog
yy,xx=np.mgrid[0:HT,0:WD]; cx,cy=WD*0.52,HT*0.5
dist=np.hypot((xx-cx)/560,(yy-cy)/330)
blob=np.asarray(Image.fromarray((noise(20,9)*255).astype(np.uint8)).resize((WD,HT),Image.BICUBIC),dtype=float)/255
m=np.clip((1.0-dist+ (blob-0.5)*0.5)*6,0,1)
# block-step the explore edge (chunks explored in blocks)
mb=Image.fromarray((m*255).astype(np.uint8)).resize((WD//64,HT//64),Image.BILINEAR).resize((WD,HT),Image.NEAREST)
mask=Image.fromarray((m*255).astype(np.uint8)); mask=Image.fromarray(np.minimum(np.asarray(mask),np.asarray(mb)).astype(np.uint8)).filter(ImageFilter.GaussianBlur(6))
fogt=Image.open(ART+"textures/fog_tile_64.png").convert("RGB")
fog=Image.new("RGB",(WD,HT))
for x in range(0,WD,128):
    for y in range(0,HT,128): fog.paste(fogt.resize((128,128),Image.NEAREST).point(lambda v:int(v*0.55)),(x,y))
img=Image.composite(terrain,fog,mask).convert("RGBA")
ov=Image.new("RGBA",(WD,HT),(0,0,0,0)); d=ImageDraw.Draw(ov)

# ---- chunk grid (16 blocks = 64px), faint
for x in range(0,WD,64): d.line([(x,0),(x,HT)],fill=(255,255,255,22))
for y in range(0,HT,64): d.line([(0,y),(WD,y)],fill=(255,255,255,22))

# ---- claims: outlines in owner color, faint tint
def claim(cells,colr,name):
    s=set(cells)
    for (i,j) in s: d.rectangle([i*64,j*64,i*64+63,j*64+63],fill=colr+(28,))
    for (i,j) in s:
        x,y=i*64,j*64
        if (i,j-1) not in s: d.line([(x,y),(x+64,y)],fill=colr+(230,),width=3)
        if (i,j+1) not in s: d.line([(x,y+63),(x+64,y+63)],fill=colr+(230,),width=3)
        if (i-1,j) not in s: d.line([(x,y),(x,y+64)],fill=colr+(230,),width=3)
        if (i+1,j) not in s: d.line([(x+63,y),(x+63,y+64)],fill=colr+(230,),width=3)
    i0=min(c[0] for c in s); j0=min(c[1] for c in s)
    d.text((i0*64+8,j0*64+6),name,font=F(14,True),fill=colr+(255,),stroke_width=3,stroke_fill=(10,14,18,220))
claim([(10,5),(11,5),(10,6),(11,6),(12,6)],(57,255,140),"Tempest's Hold")
claim([(6,7),(7,7),(7,8)],(90,170,255),"Leaf's Farm")
img.alpha_composite(ov); d=ImageDraw.Draw(img)

# ---- marks with name + distance
def mark(fn,x,y,name,dist):
    ic=Image.open(ART+"marks/"+fn).convert("RGBA"); img.alpha_composite(ic,(x-ic.width//2,y-ic.height//2))
    f=F(13,True); tw=d.textlength(name,font=f)
    d.text((x-tw/2,y-34),name,font=f,fill=W,stroke_width=3,stroke_fill=(10,14,18,230))
    f2=MONO(11); tw=d.textlength(dist,font=f2)
    d.text((x-tw/2,y+18),dist,font=f2,fill=(180,200,190),stroke_width=2,stroke_fill=(10,14,18,230))
mark("mark_home_32.png",705,385,"Home","142m")
mark("mark_mine_32.png",470,250,"Iron Mine","318m")
mark("mark_portal_32.png",880,520,"Nether Portal","402m")
mark("mark_skull_32.png",330,470,"Spawner","455m")
mark("mark_flag_32.png",960,300,"Meeting Point","388m")
mark("mark_death_32.png",560,600,"Death point","290m")
# waystone (server marker)
ws=SM["hexbadge"](32,SM["waystone"],(120,170,255)); img.alpha_composite(ws,(1040-16,450-16))
d.text((1040-d.textlength("Riverside",font=F(13,True))/2,450-34),"Riverside",font=F(13,True),fill=W,stroke_width=3,stroke_fill=(10,14,18,230))
# players
oth=Image.open(ART+"players/arrow_other_32.png").convert("RGBA"); img.alpha_composite(oth,(800-16,240-16))
d.text((800-d.textlength("Leaf",font=F(12,True))/2,240-30),"Leaf",font=F(12,True),fill=W,stroke_width=3,stroke_fill=(10,14,18,230))
me=Image.open(ART+"players/arrow_player_32.png").convert("RGBA").rotate(-35,resample=Image.BICUBIC,expand=True)
img.alpha_composite(me,(int(cx)-me.width//2,int(cy)-me.height//2))
# cursor crosshair
mx,my=860,410
d.line([(mx-9,my),(mx-3,my)],fill=W,width=2); d.line([(mx+3,my),(mx+9,my)],fill=W,width=2)
d.line([(mx,my-9),(mx,my-3)],fill=W,width=2); d.line([(mx,my+3),(mx,my+9)],fill=W,width=2)

# ---- vignette toward page edges
vig=Image.new("L",(WD,HT),0); vd=ImageDraw.Draw(vig)
for i in range(60): vd.rectangle([i,i,WD-i,HT-i],outline=int(150*(1-i/60)**2))
img.alpha_composite(Image.merge("RGBA",(*Image.new("RGB",(WD,HT),(10,14,18)).split(),vig)))

d=ImageDraw.Draw(img,"RGBA")
def brackets(x0,y0,x1,y1,b=12,w=2,c=GREEN):
    for x,y,sx,sy in [(x0,y0,1,1),(x1,y0,-1,1),(x0,y1,1,-1),(x1,y1,-1,-1)]:
        d.line([(x,y),(x+sx*b,y)],fill=c,width=w); d.line([(x,y),(x,y+sy*b)],fill=c,width=w)

# ---- left index rail
RW=60
d.rectangle([0,0,RW,HT],fill=SLATE+(238,)); d.line([(RW,0),(RW,HT)],fill=GREEN+(90,),width=1)
seal=[(RW//2,14),(RW//2+12,26),(RW//2,38),(RW//2-12,26)]
d.polygon(seal,outline=GREEN,width=2); d.polygon([(RW//2,21),(RW//2+5,26),(RW//2,31),(RW//2-5,26)],fill=GREEN)
y=60
d.text((10,y),"WORLDS",font=F(9,True),fill=MUTE); y+=16
for i,(n,c) in enumerate([("overworld",(57,255,140)),("nether",(255,90,70)),("end",(190,120,255)),("modded",(255,190,70))]):
    t=D["tab"](40,D[n],c); img.alpha_composite(t,(RW//2-20,y))
    if i==0: d.rectangle([0,y+4,3,y+36],fill=GREEN)
    y+=48
y+=8; d.line([(12,y),(RW-12,y)],fill=GREEN+(60,)); y+=12
d.text((10,y),"CODEX",font=F(9,True),fill=MUTE); y+=16
for fn in [G["marks"],A["copy"],A["settings"]]:
    pass
icons=[G["chip"](40,G["marks"],True),A["act_chip"](40,A["copy"]),A["act_chip"](40,A["printer"]),A["act_chip"](40,A["settings"])]
for ic in icons: img.alpha_composite(ic,(RW//2-20,y)); y+=48
# hover label for Nether tab
hy=60+16+48
d.rectangle([RW+8,hy+8,RW+8+86,hy+32],fill=DARK+(240,)); brackets(RW+8,hy+8,RW+94,hy+32,6,1)
d.text((RW+18,hy+12),"The Nether",font=F(12,True),fill=W)

# ---- page header (top, no bar: floating title with brackets)
hx0,hy0=RW+20,16
d.rectangle([hx0,hy0,hx0+300,hy0+44],fill=DARK+(225,)); brackets(hx0,hy0,hx0+300,hy0+44)
img.alpha_composite(D["tab"](28,D["overworld"],GREEN),(hx0+10,hy0+8))
d.text((hx0+46,hy0+6),"WORLD MAP",font=F(15,True),fill=GREEN)
d.text((hx0+46,hy0+25),"Overworld  ·  Survival World",font=F(11),fill=MUTE)
# search + lens top-right
sx=WD-60-20-260
d.rectangle([sx,hy0,sx+220,hy0+44],fill=DARK+(225,)); brackets(sx,hy0,sx+220,hy0+44)
img.alpha_composite(A["act_chip"](28,A["search"]),(sx+8,hy0+8))
d.text((sx+44,hy0+14),"Search marks, claims…",font=F(12),fill=(110,122,130))
lx=sx+232
d.rectangle([lx,hy0,lx+44,hy0+44],fill=DARK+(225,)); brackets(lx,hy0,lx+44,hy0+44)
img.alpha_composite(A["act_chip"](28,A["lens"]),(lx+8,hy0+8))

# ---- right rail: layers
RX=WD-60
d.rectangle([RX,0,WD,HT],fill=SLATE+(238,)); d.line([(RX,0),(RX,HT)],fill=GREEN+(90,),width=1)
y=16; d.text((RX+10,y),"LAYERS",font=F(9,True),fill=MUTE); y+=16
for n,on in [("claims",1),("marks",1),("players",1),("grid",1),("slime",0),("waystones",1),("homes",0)]:
    img.alpha_composite(G["chip"](40,G[n],bool(on)),(RX+10,y)); y+=48
# keycap hints
y+=6; d.line([(RX+12,y),(WD-12,y)],fill=GREEN+(60,)); y+=12
d.text((RX+14,y),"KEYS",font=F(9,True),fill=MUTE); y+=16
for k,lab in [("RMB","Actions"),("Drag","Pan"),("Wheel","Zoom"),("Esc","Close")]:
    d.rounded_rectangle([RX+8,y,RX+52,y+24],radius=4,fill=(30,38,44),outline=(80,92,100))
    d.line([(RX+10,y+22),(RX+50,y+22)],fill=(10,14,18),width=2)
    d.text((RX+30-d.textlength(k,font=F(10,True))/2,y+5),k,font=F(10,True),fill=W)
    d.text((RX+30-d.textlength(lab,font=F(9))/2,y+26),lab,font=F(9),fill=MUTE); y+=46

# ---- bottom readout strip (no box: hairline + text)
by=HT-34
d.rectangle([RW+1,by,RX-1,HT],fill=(10,14,18,200)); d.line([(RW,by),(RX,by)],fill=GREEN+(110,),width=1)
items=[("X","1240"),("Y","72"),("Z","-385"),("","Plains"),("","Chunk 77, -25"),("","Claimed: Tempest's Hold")]
x=RW+20
for k,v in items:
    if k: d.text((x,by+9),k,font=F(12,True),fill=GREEN); x+=14
    d.text((x,by+9),v,font=MONO(12) if k else F(12),fill=W); x+=d.textlength(v,font=MONO(12) if k else F(12))+22
    if not k and v!=items[-1][1]: d.text((x-14,by+8),"·",font=F(12),fill=MUTE)
# zoom scale right
zs="1 : 4"; d.text((RX-20-d.textlength(zs,font=MONO(12)),by+9),zs,font=MONO(12),fill=MUTE)
sbx=RX-100; d.line([(sbx-64,by+16),(sbx,by+16)],fill=W,width=2); d.line([(sbx-64,by+12),(sbx-64,by+20)],fill=W,width=2); d.line([(sbx,by+12),(sbx,by+20)],fill=W,width=2)
d.text((sbx-48,by+18),"16 blocks",font=F(8),fill=MUTE)

# compass N at top of map area
nx=(RW+RX)//2
d.polygon([(nx-12,74),(nx+12,74),(nx+12,92),(nx,100),(nx-12,92)],fill=DARK+(230,),outline=GREEN)
d.text((nx-d.textlength("N",font=F(12,True))/2,76),"N",font=F(12,True),fill=GREEN)

img.convert("RGB").save("ui_world_map.png"); print("ok")
