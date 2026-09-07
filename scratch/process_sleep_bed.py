import os
from PIL import Image
import numpy as np

brain_dir = r'C:\Users\19836\.gemini\antigravity\brain\60f08532-a4e7-4590-8e99-b331ad24bf89\.user_uploaded'
pet_dir = r'd:\heming\shike\shike-frontend\images\pet'
sprites_dir = r'd:\heming\shike\shike-frontend\images\pet_sprites'
out_gif_dir = r'd:\heming\shike\scratch\gifs'

os.makedirs(sprites_dir, exist_ok=True)
os.makedirs(out_gif_dir, exist_ok=True)

# Copy to pet folder for archive
src_path = os.path.join(brain_dir, 'media_1787885052515.jpg')
im = Image.open(src_path)
im.save(os.path.join(pet_dir, '猫窝睡觉.jpg'), 'JPEG', quality=95)

def remove_white_bg(im, threshold=242):
    im = im.convert('RGBA')
    arr = np.array(im)
    r, g, b, a = arr[:,:,0].astype(float), arr[:,:,1].astype(float), arr[:,:,2].astype(float), arr[:,:,3]
    white_mask = (r > threshold) & (g > threshold) & (b > threshold)
    avg = (r + g + b) / 3.0
    alpha = np.clip(255.0 - (avg - threshold) / (255.0 - threshold) * 255.0, 0.0, 255.0).astype(np.uint8)
    arr[:,:,3] = np.where(white_mask, alpha, 255)
    return Image.fromarray(arr)

def auto_trim(im, padding=8):
    arr = np.array(im)
    alpha = arr[:,:,3]
    active = np.where(alpha > 10)
    if len(active[0]) == 0 or len(active[1]) == 0:
        return im
    y0, y1 = max(0, active[0].min() - padding), min(im.height, active[0].max() + padding)
    x0, x1 = max(0, active[1].min() - padding), min(im.width, active[1].max() + padding)
    return im.crop((x0, y0, x1, y1))

# 1. Extract empty bed (top-left or mid-left)
empty_bed_raw = im.crop((20, 40, 285, 265))
empty_bed_rgba = auto_trim(remove_white_bg(empty_bed_raw))
empty_bed_rgba.save(os.path.join(sprites_dir, 'bed_empty.png'))
print('Saved bed_empty.png:', empty_bed_rgba.size)

# 2. Extract sleep sequence 3 frames
sleep_boxes = [
    (290, 275, 555, 490),  # Frame 0: curled, z bubble
    (290, 480, 555, 725),  # Frame 1: breathing, zZ bubble, stars
    (290, 715, 555, 965),  # Frame 2: smiling deep sleep, zZZ bubble
]

sleep_frames = []
for idx, box in enumerate(sleep_boxes):
    sub = im.crop(box)
    rgba = auto_trim(remove_white_bg(sub))
    rgba.save(os.path.join(sprites_dir, f'cat_sleep_{idx}.png'))
    sleep_frames.append(rgba)
    print(f'Saved cat_sleep_{idx}.png:', rgba.size)

# 3. Create animated sleep GIF (0 -> 1 -> 2 -> 1 -> 0 loop)
loop_frames = [sleep_frames[0], sleep_frames[1], sleep_frames[2], sleep_frames[1]]
max_w = max(f.width for f in loop_frames)
max_h = max(f.height for f in loop_frames)

padded_frames = []
for f in loop_frames:
    canv = Image.new('RGBA', (max_w, max_h), (255, 255, 255, 0))
    canv.paste(f, ((max_w - f.width) // 2, max_h - f.height), f)
    padded_frames.append(canv)

gif_path = os.path.join(out_gif_dir, 'anim_sleep.gif')
padded_frames[0].save(
    gif_path,
    save_all=True,
    append_images=padded_frames[1:],
    duration=400, # 400ms per frame = slow soothing breathing
    loop=0,
    disposal=2
)
print(f'Generated anim_sleep.gif successfully: {padded_frames[0].size}')
