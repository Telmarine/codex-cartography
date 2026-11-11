import math, numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont
GREEN=(57,255,140); SLATE=(22,27,33); DARK=(14,18,22)
SZ=256; S=4; N=SZ*S
def plate_pts(o,w,k): return [(o+k,o),(o+w-k,o),(o+w,o+k),(o+w,o+w-k),(o+w-k,o+w),(o+k,o+w),(o,o+w-k),(o,o+k)]
def frame(shape):
    im=Image.new("RGBA",(N,N),(0,0,0,0)); band=18*S; inner=N-2*band
    outer=Image.new("L",(N,N),0); od=ImageDraw.Draw(outer)
    hole=Image.new("L",(N,N),0); hd=ImageDraw.Draw(hole)
    if shape=="square":
        od.rectangle([4*S,4*S,N-4*S,N-4*S],fill=255); hd.rectangle([band,band,N-band,N-band],fill=255)
    elif shape=="round":
        od.ellipse([4*S,4*S,N-4*S,N-4*S],fill=255); hd.ellipse([band,band,N-band,N-band],fill=255)
    else:
        od.polygon(plate_pts(4*S,N-8*S,46*S),fill=255); hd.polygon(plate_pts(band,inner,36*S),fill=255)
    ring=Image.fromarray(np.minimum(np.array(outer),255-np.array(hole)).astype(np.uint8))
    ga=np.array(ring.filter(ImageFilter.GaussianBlur(6*S))).astype(float)*0.5*(1-np.array(hole)/255.0)
    glow=Image.new("RGBA",(N,N),GREEN+(0,)); glow.putalpha(Image.fromarray(ga.astype(np.uint8)))
    im=Image.alpha_composite(im,glow)
    body=Image.new("RGBA",(N,N),SLATE+(255,)); im.paste(body,(0,0),ring)
    d=ImageDraw.Draw(im)
    # hairlines: outer edge and inner edge
    if shape=="square":
        d.rectangle([4*S,4*S,N-4*S,N-4*S],outline=GREEN+(140,),width=S); d.rectangle([band,band,N-band,N-band],outline=GREEN,width=S)
        for x,y,sx,sy in [(band-6*S,band-6*S,1,1),(N-band+6*S,band-6*S,-1,1),(band-6*S,N-band+6*S,1,-1),(N-band+6*S,N-band+6*S,-1,-1)]:
            d.line([(x,y),(x+sx*26*S,y)],fill=GREEN,width=2*S); d.line([(x,y),(x,y+sy*26*S)],fill=GREEN,width=2*S)
    elif shape=="round":
        d.ellipse([4*S,4*S,N-4*S,N-4*S],outline=GREEN+(140,),width=S); d.ellipse([band,band,N-band,N-band],outline=GREEN,width=S)
        c=N/2
        for a in range(0,360,15):
            r1=N/2-band+2*S; r2=r1+(9*S if a%90==0 else 4*S); t=math.radians(a)
            d.line([(c+r1*math.cos(t),c+r1*math.sin(t)),(c+r2*math.cos(t),c+r2*math.sin(t))],fill=GREEN if a%90==0 else GREEN+(120,),width=2*S if a%90==0 else S)
    else:
        p=plate_pts(4*S,N-8*S,46*S); d.line(p+[p[0]],fill=GREEN+(140,),width=S)
        p2=plate_pts(band,inner,36*S); d.line(p2+[p2[0]],fill=GREEN,width=S)
        for i in range(0,8,2):   # bracket accents on the clipped corners
            a=p[i-1] if i else p[7]; b=p[i]
            pass
        b=22*S
        for x,y,sx in [(band+36*S,band-7*S,1),(N-band-36*S,band-7*S,-1),(band+36*S,N-band+7*S,1),(N-band-36*S,N-band+7*S,-1)]:
            d.line([(x,y),(x+sx*b,y)],fill=GREEN,width=2*S)
    # N tab at top center
    tw=22*S; d.rectangle([N/2-tw/2,2*S,N/2+tw/2,band-2*S],fill=DARK,outline=GREEN,width=S)
    f=ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf",13*S)
    d.text((N/2,band/2),"N",font=f,fill=GREEN,anchor="mm")
    return im.resize((SZ,SZ),Image.LANCZOS)
for sh in ["plate","square","round"]: frame(sh).save(f"frame_{sh}_256.png")
# preview over terrain
ter=Image.open("cartography_minimap_example.png").crop((40,40,380,380)).resize((256,256))
sheet=Image.new("RGBA",(3*290+20,330),(8,10,12,255)); d=ImageDraw.Draw(sheet)
F=ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",12)
masks={}
for i,sh in enumerate(["plate","square","round"]):
    fr=Image.open(f"frame_{sh}_256.png"); x=20+i*290
    m=Image.new("L",(256,256),0); md=ImageDraw.Draw(m)
    if sh=="square": md.rectangle([18,18,238,238],fill=255)
    elif sh=="round": md.ellipse([18,18,238,238],fill=255)
    else: md.polygon(plate_pts(18,220,36),fill=255)
    tile=Image.new("RGBA",(256,256),(0,0,0,0)); tile.paste(ter,(0,0),m)
    tile=Image.alpha_composite(tile,fr); sheet.alpha_composite(tile,(x,20))
    d.text((x,290),{"plate":"codex plate (default)","square":"square","round":"round"}[sh],font=F,fill=(140,160,150))
sheet.convert("RGB").save("frames_preview.png"); print("ok")
