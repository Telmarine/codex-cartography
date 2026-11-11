import os, math, numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageEnhance
SRC="/mnt/user-data/outputs/cartography-art/src/"; ART="/mnt/user-data/outputs/cartography-art/"
def load(name):
    s=open(SRC+name).read(); s=s[:s.index("\nSET")]
    cwd=os.getcwd(); os.chdir(SRC); ns={}; exec(s,ns); os.chdir(cwd); return ns
G=load("glyphs.py"); A=load("actions.py")
GREEN=(57,255,140); DARK=(14,18,22); W=(232,240,236); MUTE=(130,142,150); GREY=(96,104,110)
F=lambda s,b=False: ImageFont.truetype("/usr/share/fonts/opentype/inter/Inter-%s.otf"%("SemiBold" if b else "Medium"),s)
img=Image.open(ART+"ui/ui_world_map.png").convert("RGBA"); WD,HT=img.size
# the chunk under the cursor
cx,cy=860,410
ch=(13*64,6*64)
ov=Image.new("RGBA",(WD,HT),(0,0,0,0)); od=ImageDraw.Draw(ov)
od.rectangle([ch[0],ch[1],ch[0]+63,ch[1]+63],fill=(57,255,140,50))
for k in range(0,64,8):
    od.line([(ch[0]+k,ch[1]),(ch[0]+k+4,ch[1])],fill=GREEN+(255,),width=2); od.line([(ch[0]+k,ch[1]+63),(ch[0]+k+4,ch[1]+63)],fill=GREEN+(255,),width=2)
    od.line([(ch[0],ch[1]+k),(ch[0],ch[1]+k+4)],fill=GREEN+(255,),width=2); od.line([(ch[0]+63,ch[1]+k),(ch[0]+63,ch[1]+k+4)],fill=GREEN+(255,),width=2)
img.alpha_composite(ov)
img=Image.alpha_composite(img,Image.new("RGBA",(WD,HT),(6,10,14,70)))
def glyph(fn,size,col=W):
    N=size*8; im=Image.new("RGBA",(N,N),(0,0,0,0)); dd=ImageDraw.Draw(im); fn(dd,N/2,N*0.04,col); return im.resize((size,size),Image.LANCZOS)
def flagclaim(d,c,u,f):   # claim: chunk square with a plus
    m=5.6*u; d.rectangle([c-m,c-m,c+m,c+m],outline=f,width=int(1.2*u))
    d.line([(c-3*u,c),(c+3*u,c)],fill=f,width=int(1.3*u)); d.line([(c,c-3*u),(c,c+3*u)],fill=f,width=int(1.3*u))
def unclaim(d,c,u,f):
    m=5.6*u; d.rectangle([c-m,c-m,c+m,c+m],outline=f,width=int(1.2*u)); d.line([(c-3*u,c),(c+3*u,c)],fill=f,width=int(1.3*u))
slices=[("New mark here",A["newmark"],True),("Claim this chunk",flagclaim,True),("Unclaim",unclaim,False),
        ("Travel here",A["travel"],True),("Copy coordinates",A["copy"],True)]
n=len(slices); R0,R1=54,150; gap=2.5; sel=1
ov=Image.new("RGBA",(WD,HT),(0,0,0,0)); od=ImageDraw.Draw(ov)
for i,(name,fn,en) in enumerate(slices):
    a0=-90-180/n+i*360/n+gap; a1=a0+360/n-2*gap
    pts=[(cx+R1*math.cos(math.radians(a)),cy+R1*math.sin(math.radians(a))) for a in np.linspace(a0,a1,30)]
    pts+=[(cx+R0*math.cos(math.radians(a)),cy+R0*math.sin(math.radians(a))) for a in np.linspace(a1-gap*1.5,a0+gap*1.5,20)]
    on=(i==sel)
    od.polygon(pts,fill=(24,52,40,240) if on else (14,18,22,232),outline=GREEN+(255,) if on else ((57,255,140,70) if en else (90,96,100,90)),width=2 if on else 1)
img.alpha_composite(ov)
g=Image.new("RGBA",(WD,HT),(0,0,0,0)); gd=ImageDraw.Draw(g)
am=-90+sel*360/n; gd.arc([cx-R1-2,cy-R1-2,cx+R1+2,cy+R1+2],am-180/n+gap,am+180/n-gap,fill=GREEN+(200,),width=6)
img.alpha_composite(g.filter(ImageFilter.GaussianBlur(4))); d=ImageDraw.Draw(img)
for i,(name,fn,en) in enumerate(slices):
    a=math.radians(-90+i*360/n); rm=(R0+R1)/2; gx,gy=cx+rm*math.cos(a),cy+rm*math.sin(a)
    on=(i==sel); col=GREEN if on else (W if en else GREY)
    gi=glyph(fn,44,col); img.alpha_composite(gi,(int(gx-22),int(gy-30)))
    f=F(10,True); 
    for li,line in enumerate(name.split(" ",1) if len(name)>12 else [name]):
        tw=d.textlength(line,font=f); d.text((gx-tw/2,gy+12+li*12),line,font=f,fill=col)
d.ellipse([cx-R0+6,cy-R0+6,cx+R0-6,cy+R0-6],fill=(10,14,18,245),outline=(57,255,140,90))
for t,yy,f,c in [("Chunk",cy-22,F(10),MUTE),("77, -25",cy-8,F(13,True),W),("14 / 25 used",cy+10,F(10),GREEN)]:
    d.text((cx-d.textlength(t,font=f)/2,yy),t,font=f,fill=c)
def brackets(x0,y0,x1,y1,b=12,w=2,c=GREEN):
    for x,y,sx,sy in [(x0,y0,1,1),(x1,y0,-1,1),(x0,y1,1,-1),(x1,y1,-1,-1)]:
        d.line([(x,y),(x+sx*b,y)],fill=c,width=w); d.line([(x,y),(x,y+sy*b)],fill=c,width=w)
def tag(x,y,lines,c=GREEN):
    f=F(12,True); tw=max(d.textlength(l,font=f) for l in lines)
    h=8+18*len(lines); d.rectangle([x,y,x+tw+20,y+h],fill=DARK); brackets(x,y,x+tw+20,y+h,6,1,c)
    for k,l in enumerate(lines): d.text((x+10,y+4+k*18),l,font=f if k==0 else F(11),fill=W if k==0 else MUTE)
# info card for the picked slice
tag(cx-170,cy-R1-86,["Claim this chunk","Uses 1 of your 25 chunks (rank: Member)","Next to Tempest's Hold, so it joins that claim"])
# disabled slice reason
ang=math.radians(-90+2*360/n); gx,gy=cx+(R1+10)*math.cos(ang),cy+(R1+10)*math.sin(ang)
tag(int(gx)+6,int(gy)-6,["Unclaim","This chunk isn't claimed"],(120,128,134))
img.convert("RGB").save("ui_map_radial.png"); print("ok")
