import math
from PIL import Image, ImageDraw, ImageFilter, ImageFont
GREEN=(57,255,140); DARK=(14,18,22); W=(232,240,236); EDGE=GREEN
def chip(size, draw, on=True):
    # glyph button: square chip with corner brackets; green when on, grey when off
    S=8; N=size*S; im=Image.new("RGBA",(N,N),(0,0,0,0)); d=ImageDraw.Draw(im); c=N/2
    col=GREEN if on else (90,100,108)
    m=N*0.06
    d.rectangle([m,m,N-m,N-m],fill=DARK+(240,))
    d.rectangle([m,m,N-m,N-m],outline=col+(90,),width=int(N*0.02))
    b=N*0.24; w=int(N*0.05)
    for x,y,sx,sy in [(m,m,1,1),(N-m,m,-1,1),(m,N-m,1,-1),(N-m,N-m,-1,-1)]:
        d.line([(x,y),(x+sx*b,y)],fill=col,width=w); d.line([(x,y),(x,y+sy*b)],fill=col,width=w)
    draw(d,c,N*0.04,W if on else (120,128,134))
    return im.resize((size,size),Image.LANCZOS)
def claims(d,c,u,f):
    # dashed square outline
    x0,y0,x1,y1=c-5*u,c-5*u,c+5*u,c+5*u
    for (a,b) in [((x0,y0),(x1,y0)),((x1,y0),(x1,y1)),((x1,y1),(x0,y1)),((x0,y1),(x0,y0))]:
        L=math.hypot(b[0]-a[0],b[1]-a[1]); n=0
        while n<L:
            e=min(n+2.2*u,L); d.line([(a[0]+(b[0]-a[0])*n/L,a[1]+(b[1]-a[1])*n/L),(a[0]+(b[0]-a[0])*e/L,a[1]+(b[1]-a[1])*e/L)],fill=f,width=int(1.1*u)); n+=3.4*u
    d.rectangle([c-2*u,c-2*u,c+2*u,c+2*u],fill=f)
def marks(d,c,u,f):
    r=5*u; d.polygon([(c,c-r),(c+r,c),(c,c+r),(c-r,c)],outline=f,width=int(1.1*u))
    d.polygon([(c,c-2*u),(c+2*u,c),(c,c+2*u),(c-2*u,c)],fill=f)
def players(d,c,u,f):
    d.ellipse([c-2.2*u,c-5.4*u,c+2.2*u,c-1.0*u],fill=f)
    d.pieslice([c-4.6*u,c-0.2*u,c+4.6*u,c+9.0*u],start=180,end=360,fill=f)
def grid(d,c,u,f):
    for i in range(4):
        x=c-5*u+i*(10*u/3); d.line([(x,c-5*u),(x,c+5*u)],fill=f,width=int(0.7*u)); d.line([(c-5*u,x-c+c),(c+5*u,x-c+c)],fill=f,width=int(0.7*u))
def slime(d,c,u,f):
    SL=(120,220,110)
    d.rounded_rectangle([c-4.8*u,c-4.0*u,c+4.8*u,c+4.8*u],radius=1.6*u,fill=SL if f==W else f)
    d.rectangle([c-2.6*u,c-1.4*u,c-1.2*u,c+0.2*u],fill=DARK); d.rectangle([c+1.2*u,c-1.4*u,c+2.6*u,c+0.2*u],fill=DARK)
    d.rectangle([c-0.8*u,c+2.0*u,c+0.8*u,c+2.8*u],fill=DARK)
def waystones(d,c,u,f):
    # tall stone with a glowing rune
    d.polygon([(c-2.6*u,c+5.4*u),(c-3.2*u,c-2.8*u),(c,c-5.6*u),(c+3.2*u,c-2.8*u),(c+2.6*u,c+5.4*u)],fill=f)
    d.polygon([(c,c-2.4*u),(c+1.2*u,c-0.4*u),(c,c+1.6*u),(c-1.2*u,c-0.4*u)],fill=(57,255,140) if f==W else DARK)
    d.rectangle([c-4.2*u,c+4.6*u,c+4.2*u,c+5.6*u],fill=f)
def homes(d,c,u,f):
    # house outline + small flag (homes & warps)
    d.polygon([(c-1*u,c-3.6*u),(c+3.8*u,c+0.6*u),(c-5.8*u,c+0.6*u)],fill=f)
    d.rectangle([c-4.6*u,c+0.2*u,c+2.6*u,c+5.2*u],fill=f)
    d.rectangle([c-2.0*u,c+2.0*u,c,c+5.2*u],fill=DARK)
    d.line([(c+4.2*u,c+5.2*u),(c+4.2*u,c-5.4*u)],fill=f,width=int(0.8*u))
    d.polygon([(c+4.2*u,c-5.4*u),(c+6.4*u,c-4.4*u),(c+4.2*u,c-3.4*u)],fill=f)
SET=[("claims",claims),("marks",marks),("players",players),("grid",grid),("slime",slime),("waystones",waystones),("homes",homes)]
for n,f in SET:
    chip(24,f,True).save(f"layer_{n}_on_24.png"); chip(24,f,False).save(f"layer_{n}_off_24.png")
sheet=Image.new("RGBA",(7*120+20,240),(8,10,12,255)); d=ImageDraw.Draw(sheet)
F=ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",12)
labels={"homes":"homes & warps","grid":"chunk grid","slime":"slime chunks"}
for i,(n,f) in enumerate(SET):
    x=20+i*120
    sheet.alpha_composite(chip(24,f,True).resize((96,96),Image.NEAREST),(x,14))
    sheet.alpha_composite(Image.open(f"layer_{n}_on_24.png"),(x,124)); sheet.alpha_composite(Image.open(f"layer_{n}_off_24.png"),(x+34,124))
    d.text((x,156),labels.get(n,n),font=F,fill=(140,160,150))
d.text((20,196),"layer glyphs for the world map's right rail · 24 px · left = on, right = off",font=F,fill=(140,160,150))
sheet.convert("RGB").save("layers_preview.png"); print("ok")
