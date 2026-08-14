from pathlib import Path
from PIL import Image

root = Path('/home/ubuntu/frostsoulx-repo/app/src/main/res')
source = Path('/home/ubuntu/frostsoulx-repo/tools/assets/frostsoul_launcher_logo.png')
base = Image.open(source).convert('RGBA')

sizes = {
    'mipmap-mdpi': 108,
    'mipmap-hdpi': 162,
    'mipmap-xhdpi': 216,
    'mipmap-xxhdpi': 324,
    'mipmap-xxxhdpi': 432,
}

for directory, size in sizes.items():
    target = base.resize((size, size), Image.Resampling.LANCZOS)
    folder = root / directory
    target.save(folder / 'ic_launcher.png', format='PNG', optimize=True)
    target.save(folder / 'ic_launcher_round.png', format='PNG', optimize=True)
    target.save(folder / 'ic_launcher_foreground.png', format='PNG', optimize=True)
    rgb = target.convert('RGB')
    pixels = rgb.load()
    mask = Image.new('L', target.size, 0)
    mask_pixels = mask.load()
    for y in range(target.height):
        for x in range(target.width):
            red, green, blue = pixels[x, y]
            mask_pixels[x, y] = max(abs(red - green), abs(green - blue), abs(red - blue))
    monochrome = Image.new('RGBA', target.size, (255, 255, 255, 0))
    monochrome.putalpha(mask)
    monochrome.save(folder / 'ic_launcher_monochrome.png', format='PNG', optimize=True)

print('Prepared FrostSoul launcher assets for', ', '.join(sizes))
