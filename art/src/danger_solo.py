import math
from PIL import Image, ImageDraw, ImageFilter, ImageFont
DARK=(14,18,22); Y=(255,214,60)
def tri(size, glow=Y):
    S=8; N=size*S; im=Image.new("RGBA",(N,N),(0,0,0,0))
    c=N/2; L=N*0.86; h=L*math.sqrt(3)/2
    top=c-h/2; base=c+h/2
    pts=[(c,top),(c+L/2,base),(c-L/2,base)]
    g=Image.new("RGBA",(N,N),(0,0,0,0)); ImageDraw.Draw(g).polygon(pts,fill=glow+(140,))
    im=Image.alpha_composite(im,g.filter(ImageFilter.GaussianBlur(N*0.035)))
    d=ImageDraw.Draw(im)
    d.polygon(pts,fill=Y)
    d.line(pts+[pts[0]],fill=DARK,width=int(N*0.035),joint="curve")
    u=N*0.033; m=c+h/6
    d.polygon([(c-1.35*u,m-6.0*u),(c+1.35*u,m-6.0*u),(c+0.85*u,m+1.0*u),(c-0.85*u,m+1.0*u)],fill=DARK)
    d.ellipse([c-1.3*u,m+2.2*u,c+1.3*u,m+4.8*u],fill=DARK)
    return im.resize((size,size),Image.LANCZOS)
b64=tri(64); b32=tri(32); b64.save("mark_danger_64.png"); b32.save("mark_danger_32.png")
sheet=Image.new("RGBA",(560,300),(8,10,12,255))
sheet.alpha_composite(b64.resize((256,256),Image.NEAREST),(20,22))
sheet.alpha_composite(b64,(320,60)); sheet.alpha_composite(b32,(420,76))
d=ImageDraw.Draw(sheet); f=ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",12)
d.text((24,282),"x4 preview",font=f,fill=(140,160,150)); d.text((320,128),"64px",font=f,fill=(140,160,150)); d.text((420,128),"32px",font=f,fill=(140,160,150))
# on a map-ish background
bg=Image.new("RGBA",(200,110),(58,98,64,255)); bd=ImageDraw.Draw(bg); bd.rectangle([0,60,200,110],fill=(40,88,112,255))
bg.alpha_composite(b64,(68,23)); sheet.alpha_composite(bg,(320,160))
d.text((320,276),"on the map",font=f,fill=(140,160,150))
sheet.convert("RGB").save("mark_danger_preview.png"); print("ok")
