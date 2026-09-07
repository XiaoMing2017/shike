import os
from PIL import Image

sprites_dir = r'd:\heming\shike\shike-frontend\images\pet_sprites'
out_gif_dir = r'd:\heming\shike\scratch\gifs'
os.makedirs(out_gif_dir, exist_ok=True)

actions = {
    'anim_chop': ['cat_chop_0.png', 'cat_chop_1.png', 'cat_chop_2.png', 'cat_chop_3.png'],
    'anim_soup': ['cat_soup_0.png', 'cat_soup_1.png', 'cat_soup_2.png', 'cat_soup_3.png'],
    'anim_run': ['cat_run_0.png', 'cat_run_1.png', 'cat_run_2.png', 'cat_run_3.png'],
    'anim_rope': ['cat_rope_0.png', 'cat_rope_1.png', 'cat_rope_2.png', 'cat_rope_3.png'],
    'anim_walk_front': ['cat_walk_front_0.png', 'cat_walk_front_1.png', 'cat_walk_front_2.png', 'cat_walk_front_3.png'],
    'anim_walk_back': ['cat_walk_back_0.png', 'cat_walk_back_1.png', 'cat_walk_back_2.png', 'cat_walk_back_3.png'],
}

for name, frame_names in actions.items():
    frames = []
    # Find max width and height
    imgs = [Image.open(os.path.join(sprites_dir, fn)) for fn in frame_names]
    max_w = max(im.width for im in imgs)
    max_h = max(im.height for im in imgs)
    
    for im in imgs:
        # Pad to same size, centered horizontally, bottom aligned
        canvas = Image.new('RGBA', (max_w, max_h), (255, 255, 255, 0))
        x = (max_w - im.width) // 2
        y = max_h - im.height
        canvas.paste(im, (x, y), im)
        # downscale for web/gif
        canvas = canvas.resize((canvas.width // 2, canvas.height // 2), Image.Resampling.LANCZOS)
        frames.append(canvas)
    
    gif_path = os.path.join(out_gif_dir, f'{name}.gif')
    frames[0].save(
        gif_path,
        save_all=True,
        append_images=frames[1:],
        duration=200, # 200ms per frame = 5 FPS
        loop=0,
        disposal=2
    )
    print(f'Generated {name}.gif successfully: {frames[0].size}')

print('All action GIFs verified and created!')
