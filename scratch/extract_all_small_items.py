import os
from PIL import Image
import numpy as np

brain_dir = r'C:\Users\19836\.gemini\antigravity\brain\60f08532-a4e7-4590-8e99-b331ad24bf89\.user_uploaded'
sprites_dir = r'd:\heming\shike\shike-frontend\images\pet_sprites'
os.makedirs(sprites_dir, exist_ok=True)

def remove_white_bg(im, threshold=242):
    im = im.convert('RGBA')
    arr = np.array(im)
    r, g, b, a = arr[:,:,0].astype(float), arr[:,:,1].astype(float), arr[:,:,2].astype(float), arr[:,:,3]
    white_mask = (r > threshold) & (g > threshold) & (b > threshold)
    avg = (r + g + b) / 3.0
    alpha = np.clip(255.0 - (avg - threshold) / (255.0 - threshold) * 255.0, 0.0, 255.0).astype(np.uint8)
    arr[:,:,3] = np.where(white_mask, alpha, 255)
    return Image.fromarray(arr)

def auto_trim(im, padding=6):
    arr = np.array(im)
    alpha = arr[:,:,3]
    active = np.where(alpha > 10)
    if len(active[0]) == 0 or len(active[1]) == 0:
        return im
    y0, y1 = max(0, active[0].min() - padding), min(im.height, active[0].max() + padding)
    x0, x1 = max(0, active[1].min() - padding), min(im.width, active[1].max() + padding)
    return im.crop((x0, y0, x1, y1))

# 1. Extract Foods, Veggies & UI from media_1787819356830.jpg (size 571x1024)
food_sheet_path = os.path.join(brain_dir, 'media_1787819356830.jpg')
if os.path.exists(food_sheet_path):
    food_im = Image.open(food_sheet_path)
    
    food_boxes = {
        'soup_tomato.png': (10, 35, 195, 250),
        'soup_broccoli.png': (195, 35, 380, 250),
        'soup_pumpkin.png': (380, 35, 565, 250),
        'food_carrot.png': (15, 320, 165, 510),
        'food_cabbage.png': (150, 360, 305, 505),
        'food_lemon.png': (305, 360, 435, 505),
        'food_apple.png': (435, 360, 560, 505),
        'heart_pink.png': (215, 605, 355, 735),
        'star_purple.png': (375, 595, 530, 735),
        'fish_dried.png': (320, 830, 470, 905),
    }
    
    for name, box in food_boxes.items():
        sub = food_im.crop(box)
        rgba = auto_trim(remove_white_bg(sub))
        rgba.save(os.path.join(sprites_dir, name))
        print(f'Extracted {name}:', rgba.size)

# 2. Extract Props from media_1787819363986.jpg (size 571x1024)
fac_sheet_path = os.path.join(brain_dir, 'media_1787819363986.jpg')
if os.path.exists(fac_sheet_path):
    fac_im = Image.open(fac_sheet_path)
    
    fac_boxes = {
        'tile_grass.png': (15, 80, 280, 310),
        'fac_cauldron_base.png': (290, 40, 555, 310),
        'fac_chopping_stump.png': (15, 410, 280, 600),
        'fac_cabbage_crate.png': (290, 390, 555, 610),
        'fac_wheel.png': (20, 670, 275, 960),
        'fac_hammock.png': (285, 690, 560, 940)
    }
    
    for name, box in fac_boxes.items():
        sub = fac_im.crop(box)
        rgba = auto_trim(remove_white_bg(sub))
        rgba.save(os.path.join(sprites_dir, name))
        print(f'Extracted {name}:', rgba.size)

# 3. Extract Cute Accessories from media_1787819359955.jpg (size 571x1024)
acc_sheet_path = os.path.join(brain_dir, 'media_1787819359955.jpg')
if os.path.exists(acc_sheet_path):
    acc_im = Image.open(acc_sheet_path)
    acc_boxes = {
        'acc_strawberry.png': (50, 790, 180, 950),
        'acc_beret.png': (190, 800, 380, 945),
        'acc_sunflower.png': (400, 780, 535, 960)
    }
    for name, box in acc_boxes.items():
        sub = acc_im.crop(box)
        rgba = auto_trim(remove_white_bg(sub))
        rgba.save(os.path.join(sprites_dir, name))
        print(f'Extracted {name}:', rgba.size)

print('All small items and UI assets successfully extracted and transparentized!')
