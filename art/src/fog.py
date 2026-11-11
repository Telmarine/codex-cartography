import random
from PIL import Image, ImageDraw, ImageFont, ImageFilter
random.seed(11)
T=64  # tileable unexplored texture
im=Image.new("RGBA",(T,T),(12,16,20,255)); d=ImageDraw.Draw(im)
# faint grid every 8 px
for i in range(0,T,8):
    d.line([(i,0),(i,T)],fill=(22,40,32,255)); d.line([(0,i),(T,i)],fill=(22,40,32,255))
# scattered "code" glyph bits: tiny 1-3 px marks in green, a few brighter
for _ in range(70):
    x=random.randrange(0,T); y=random.randrange(0,T); k=random.random()
    col=(30,110,70,255) if k<0.8 else (57,200,120,255)
    if random.random()<0.5: d.point((x,y),fill=col)
    else: d.line([(x,y),(x,min(T-1,y+random.choice([1,2])))],fill=col)
im.save("fog_tile_64.png")
# preview: tiled 4x4 at x2, plus a fade from map into fog
big=Image.new("RGBA",(256,256)); 
for gx in range(0,256,64):
    for gy in range(0,256,64): big.paste(im,(gx,gy))
ter=Image.open("cartography_minimap_example.png").crop((40,40,296,296)).convert("RGBA")
mask=Image.new("L",(256,256),0); md=ImageDraw.Draw(mask); md.polygon([(256,0),(256,256),(40,256)],fill=255); mask=mask.filter(ImageFilter.GaussianBlur(24))
blend=Image.composite(big,ter,mask)
sheet=Image.new("RGBA",(560,300),(8,10,12,255))
sheet.alpha_composite(big.resize((256,256),Image.NEAREST),(20,20)); sheet.alpha_composite(blend,(290,20))
d=ImageDraw.Draw(sheet); f=ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",12)
d.text((20,282),"unexplored texture (tiles seamlessly)",font=f,fill=(140,160,150)); d.text((290,282),"map fading into unexplored",font=f,fill=(140,160,150))
sheet.convert("RGB").save("fog_preview.png"); print("ok")
