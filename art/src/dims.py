import math
from PIL import Image, ImageDraw, ImageFilter, ImageFont
GREEN=(57,255,140); DARK=(14,18,22); W=(232,240,236)
def tab(size, draw, col):
    # dimension glyph: clipped-corner square tab (Codex plate), hairline in the dimension's color
    S=8; N=size*S; im=Image.new("RGBA",(N,N),(0,0,0,0)); c=N/2
    k=N*0.22; m=N*0.06
    pts=[(m+k,m),(N-m-k,m),(N-m,m+k),(N-m,N-m-k),(N-m-k,N-m),(m+k,N-m),(m,N-m-k),(m,m+k)]
    g=Image.new("RGBA",(N,N),(0,0,0,0)); ImageDraw.Draw(g).polygon(pts,fill=col+(120,))
    im=Image.alpha_composite(im,g.filter(ImageFilter.GaussianBlur(N*0.035))); d=ImageDraw.Draw(im)
    d.polygon(pts,fill=DARK+(245,)); d.line(pts+[pts[0]],fill=col,width=int(N*0.045))
    draw(d,c,N*0.045,col)
    return im.resize((size,size),Image.LANCZOS)
def overworld(d,c,u,col):
    # grass block side: green top band over dirt, with sun
    d.ellipse([c+1.6*u,c-5.6*u,c+4.8*u,c-2.4*u],fill=(255,214,60))
    d.polygon([(c-5.4*u,c+4.6*u),(c-5.4*u,c+0.4*u),(c-2.6*u,c-2.8*u),(c+0.6*u,c+0.2*u),(c+2.6*u,c-1.2*u),(c+5.4*u,c+1.8*u),(c+5.4*u,c+4.6*u)],fill=(120,220,120))
def nether(d,c,u,col):
    # flame
    pts=[]
    for i in range(41):
        t=i/40*2*math.pi
        r=4.8*u*(1+0.0)
        x=math.sin(t)*3.8*u; y=-math.cos(t)*5.4*u
        if y<0: x*=1-((-y)/(5.4*u))**1.6*0.9
        pts.append((c+x,c+0.6*u+y))
    d.polygon(pts,fill=(255,120,60))
    d.ellipse([c-1.7*u,c+0.4*u,c+1.7*u,c+4.6*u],fill=(255,214,60))
def end(d,c,u,col):
    # eye of the end style: almond eye with pupil
    d.polygon([(c-6.0*u,c),(c-2.4*u,c-3.4*u),(c+2.4*u,c-3.4*u),(c+6.0*u,c),(c+2.4*u,c+3.4*u),(c-2.4*u,c+3.4*u)],fill=(150,230,200))
    d.ellipse([c-2.4*u,c-2.4*u,c+2.4*u,c+2.4*u],fill=(30,90,80))
    d.ellipse([c-0.9*u,c-0.9*u,c+0.9*u,c+0.9*u],fill=DARK)
def modded(d,c,u,col):
    # other dimension: ringed planet (ring behind, planet, ring front)
    box=[c-5.8*u,c-1.6*u,c+5.8*u,c+1.6*u]
    d.arc(box,start=180,end=360,fill=W,width=int(0.8*u))
    d.ellipse([c-3.2*u,c-3.2*u,c+3.2*u,c+3.2*u],fill=W)
    d.arc([c-3.2*u,c-3.2*u,c+3.2*u,c+3.2*u],start=200,end=250,fill=DARK,width=int(0.5*u))
    d.arc(box,start=0,end=180,fill=DARK,width=int(1.8*u))
    d.arc(box,start=0,end=180,fill=W,width=int(0.8*u))
SET=[("overworld",overworld,(120,220,120)),("nether",nether,(255,110,70)),("end",end,(190,130,255)),("modded",modded,GREEN)]
for n,f,col in SET:
    tab(32,f,col).save(f"dim_{n}_32.png"); tab(20,f,col).save(f"dim_{n}_20.png")
sheet=Image.new("RGBA",(560,230),(8,10,12,255)); d=ImageDraw.Draw(sheet)
F=ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",12)
for i,(n,f,col) in enumerate(SET):
    x=20+i*135
    sheet.alpha_composite(tab(32,f,col).resize((112,112),Image.NEAREST),(x,14))
    sheet.alpha_composite(Image.open(f"dim_{n}_32.png"),(x,140)); sheet.alpha_composite(Image.open(f"dim_{n}_20.png"),(x+44,146))
    d.text((x,188),n if n!="modded" else "other / modded",font=F,fill=(140,160,150))
sheet.convert("RGB").save("dims_preview.png"); print("ok")
