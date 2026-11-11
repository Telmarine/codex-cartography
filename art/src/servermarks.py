import math
from PIL import Image, ImageDraw, ImageFilter, ImageFont
DARK=(14,18,22); W=(232,240,236); GREEN=(57,255,140)
def hexbadge(size, glyph, col):
    # server-sourced markers use a hexagon so they never look like a player's own diamond marks
    S=8; N=size*S; im=Image.new("RGBA",(N,N),(0,0,0,0)); c=N/2; r=N*0.42
    pts=[(c+r*math.cos(math.radians(a)),c+r*math.sin(math.radians(a))) for a in range(-90,270,60)]
    g=Image.new("RGBA",(N,N),(0,0,0,0)); ImageDraw.Draw(g).polygon(pts,fill=col+(150,))
    im=Image.alpha_composite(im,g.filter(ImageFilter.GaussianBlur(N*0.04))); d=ImageDraw.Draw(im)
    r2=N*0.38; pts=[(c+r2*math.cos(math.radians(a)),c+r2*math.sin(math.radians(a))) for a in range(-90,270,60)]
    d.polygon(pts,fill=DARK+(245,)); d.line(pts+[pts[0]],fill=col,width=int(N*0.045))
    glyph(d,c,N*0.036,col)
    return im.resize((size,size),Image.LANCZOS)
def waystone(d,c,u,col):
    d.polygon([(c-2.6*u,c+5.0*u),(c-3.2*u,c-2.8*u),(c,c-5.8*u),(c+3.2*u,c-2.8*u),(c+2.6*u,c+5.0*u)],fill=W)
    d.polygon([(c,c-2.6*u),(c+1.3*u,c-0.5*u),(c,c+1.6*u),(c-1.3*u,c-0.5*u)],fill=col)
    d.rectangle([c-4.2*u,c+4.4*u,c+4.2*u,c+5.6*u],fill=W)
def nexus_home(d,c,u,col):
    d.polygon([(c,c-5.4*u),(c+5.2*u,c-0.6*u),(c-5.2*u,c-0.6*u)],fill=W)
    d.rectangle([c-3.8*u,c-1.0*u,c+3.8*u,c+5.0*u],fill=W)
    d.rectangle([c-1.2*u,c+1.6*u,c+1.2*u,c+5.0*u],fill=DARK)
def warp(d,c,u,col):
    # warp: swirl arrow loop
    d.arc([c-4.8*u,c-4.8*u,c+4.8*u,c+4.8*u],start=-60,end=230,fill=W,width=int(1.3*u))
    a=math.radians(-60); x=c+4.8*u*math.cos(a); y=c+4.8*u*math.sin(a)
    d.polygon([(x+1.8*u,y-1.2*u),(x-1.0*u,y-2.2*u),(x+0.6*u,y+1.6*u)],fill=W)
    d.ellipse([c-1.4*u,c-1.4*u,c+1.4*u,c+1.4*u],fill=W)
SET=[("waystone",waystone,(120,170,255)),("home",nexus_home,(255,190,80)),("warp",warp,(200,120,255))]
for n,f,col in SET:
    hexbadge(64,f,col).save(f"server_{n}_64.png"); hexbadge(32,f,col).save(f"server_{n}_32.png")
sheet=Image.new("RGBA",(560,260),(8,10,12,255)); d=ImageDraw.Draw(sheet)
F=ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",12)
for i,(n,f,col) in enumerate(SET):
    x=20+i*180
    sheet.alpha_composite(hexbadge(64,f,col).resize((128,128),Image.NEAREST),(x,14))
    sheet.alpha_composite(Image.open(f"server_{n}_64.png"),(x,150)); sheet.alpha_composite(Image.open(f"server_{n}_32.png"),(x+74,166))
    d.text((x,222),{"waystone":"waystone","home":"home (Nexus)","warp":"warp (Nexus)"}[n],font=F,fill=(140,160,150))
sheet.convert("RGB").save("servermarks_preview.png"); print("ok")
