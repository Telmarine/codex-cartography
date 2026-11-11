# Builds the textures the mod loads (assets/codex_cartography/textures/gui/...) from the art scripts.
# Badges are drawn white so the game can tint them with the player's picked color; emblems are separate and stay white.
import os, sys, shutil, math
from PIL import Image, ImageDraw, ImageFilter
SRC=os.path.dirname(os.path.abspath(__file__)); ART=os.path.dirname(SRC)
OUT=sys.argv[1]
def load(name, cut="\nSET"):
    s=open(os.path.join(SRC,name)).read()
    if cut in s: s=s[:s.index(cut)]
    if 'if __name__' in s: s=s[:s.index('if __name__')]
    cwd=os.getcwd(); os.chdir(SRC); ns={}; exec(s,ns); os.chdir(cwd); return ns
def out(sub,name,img):
    d=os.path.join(OUT,sub); os.makedirs(d,exist_ok=True); img.save(os.path.join(d,name))
WHITE=(255,255,255)
E=load("emblems.py")
out("mark","badge.png",E["badge"](64,lambda d,c,u:None,WHITE))
def emblem(fn,size=64):
    N=size*8; im=Image.new("RGBA",(N,N),(0,0,0,0)); fn(ImageDraw.Draw(im),N/2,N*0.033); return im.resize((size,size),Image.LANCZOS)
for n in ["keep","home","village","farm","mine","treasure","shrine","ruin","skull","gate","flag","star","question","portal","ancient_city","ocean_monument"]:
    out("mark","emblem_"+n+".png",emblem(E[n]))
shutil.copy(os.path.join(ART,"marks","mark_death_64.png"),os.path.join(OUT,"mark","death.png"))
shutil.copy(os.path.join(ART,"marks","mark_danger_64.png"),os.path.join(OUT,"mark","danger.png"))
SM=load("servermarks.py")
out("server","badge.png",SM["hexbadge"](64,lambda d,c,u,col:None,WHITE))
for n,fn in [("waystone",SM["waystone"]),("home",SM["nexus_home"]),("warp",SM["warp"])]:
    N=512; im=Image.new("RGBA",(N,N),(0,0,0,0)); fn(ImageDraw.Draw(im),N/2,N*0.036,WHITE); out("server","emblem_"+n+".png",im.resize((64,64),Image.LANCZOS))
for sub,dst in [("layers","layer"),("actions","action"),("dimensions","dim"),("sweep","sweep"),("players","player"),("frames","frame")]:
    for f in os.listdir(os.path.join(ART,sub)):
        if not f.endswith(".png") or "preview" in f: continue
        name=f
        for p in ("layer_","action_","dim_","sweep_","arrow_","frame_"):
            if name.startswith(p): name=name[len(p):]
        os.makedirs(os.path.join(OUT,dst),exist_ok=True); shutil.copy(os.path.join(ART,sub,f),os.path.join(OUT,dst,name))
shutil.copy(os.path.join(ART,"textures","fog_tile_64.png"),os.path.join(OUT,"fx_fog.png"))
# white glow and drip for in-world marks (tinted in game)
N=64*8; g=Image.new("L",(N,N),0); gd=ImageDraw.Draw(g)
for r in range(N//2,0,-4): gd.ellipse([N/2-r,N/2-r,N/2+r,N/2+r],fill=int(255*(1-r/(N/2))**2))
glow=Image.new("RGBA",(N,N),(255,255,255,0)); glow.putalpha(g.filter(ImageFilter.GaussianBlur(12))); out("","fx_glow.png",glow.resize((64,64),Image.LANCZOS))
dr=Image.new("RGBA",(16,48),(255,255,255,0)); px=dr.load()
for y in range(48):
    for x in range(16):
        a=max(0.0,1-abs(x-7.5)/7.5)**1.5*(1-y/48)**1.2; px[x,y]=(255,255,255,int(220*a))
out("","fx_drip.png",dr)
print("ok")

# extra action glyphs used by the mod's pages
A=load("actions.py")
def disk(d,c,u,f):
    for yo in (-4,0,4):
        d.ellipse([c-6*u,c+yo*u-2*u,c+6*u,c+yo*u+2*u],outline=f,width=int(1.0*u))
    d.line([(c-6*u,c-4*u),(c-6*u,c+4*u)],fill=f,width=int(1.0*u)); d.line([(c+6*u,c-4*u),(c+6*u,c+4*u)],fill=f,width=int(1.0*u))
def imp(d,c,u,f):
    d.line([(c,c-7*u),(c,c+2*u)],fill=f,width=int(1.4*u)); d.polygon([(c,c+4.5*u),(c-3.2*u,c+0.8*u),(c+3.2*u,c+0.8*u)],fill=f)
    d.line([(c-7*u,c+3*u),(c-7*u,c+7*u),(c+7*u,c+7*u),(c+7*u,c+3*u)],fill=f,width=int(1.2*u))
def center(d,c,u,f):
    d.ellipse([c-5*u,c-5*u,c+5*u,c+5*u],outline=f,width=int(1.1*u))
    for a,b in [((c,c-7.5*u),(c,c-3.5*u)),((c,c+3.5*u),(c,c+7.5*u)),((c-7.5*u,c),(c-3.5*u,c)),((c+3.5*u,c),(c+7.5*u,c))]: d.line([a,b],fill=f,width=int(1.1*u))
    d.ellipse([c-1.6*u,c-1.6*u,c+1.6*u,c+1.6*u],fill=f)
def folio(d,c,u,f):
    d.polygon([(c-6.5*u,c-4*u),(c-2*u,c-4*u),(c-0.5*u,c-2.5*u),(c+6.5*u,c-2.5*u),(c+6.5*u,c+5*u),(c-6.5*u,c+5*u)],outline=f,width=int(1.1*u))
    d.line([(c-6.5*u,c-0.5*u),(c+6.5*u,c-0.5*u)],fill=f,width=int(1.0*u))
def close(d,c,u,f):
    d.line([(c-4.5*u,c-4.5*u),(c+4.5*u,c+4.5*u)],fill=f,width=int(1.5*u)); d.line([(c-4.5*u,c+4.5*u),(c+4.5*u,c-4.5*u)],fill=f,width=int(1.5*u))
def ledger(d,c,u,f):
    r=5*u; d.polygon([(c,c-r),(c+r,c),(c,c+r),(c-r,c)],outline=f,width=int(1.1*u)); d.polygon([(c,c-2*u),(c+2*u,c),(c,c+2*u),(c-2*u,c)],fill=f)
for n,fn in [("mapdata",disk),("import",imp),("center",center),("folio",folio),("close",close),("ledger",ledger)]:
    os.makedirs(os.path.join(OUT,"action"),exist_ok=True)
    A["act_chip"](24,fn).save(os.path.join(OUT,"action",n+"_24.png"))
print("extra ok")
