import os
import sys
from reportlab.lib.pagesizes import letter
from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, Image, HRFlowable, KeepTogether, PageBreak
)
from reportlab.pdfgen import canvas

class NumberedCanvas(canvas.Canvas):
    def __init__(self, *args, **kwargs):
        super(NumberedCanvas, self).__init__(*args, **kwargs)
        self._saved_page_states = []

    def showPage(self):
        self._saved_page_states.append(dict(self.__dict__))
        self._startPage()

    def save(self):
        num_pages = len(self._saved_page_states)
        for state in self._saved_page_states:
            self.__dict__.update(state)
            self.draw_page_decorations(num_pages)
            super(NumberedCanvas, self).showPage()
        super(NumberedCanvas, self).save()

    def draw_page_decorations(self, page_count):
        self.saveState()
        # Top banner line
        self.setStrokeColor(colors.HexColor("#FF7043"))
        self.setLineWidth(2)
        self.line(36, letter[1] - 36, letter[0] - 36, letter[1] - 36)

        # Header Text
        self.setFont("Helvetica-Bold", 8)
        self.setFillColor(colors.HexColor("#8D6E63"))
        self.drawString(36, letter[1] - 30, "HERBERT'S ZOOMIES • OFFICIAL GAME MANUAL")

        # Footer line
        self.setStrokeColor(colors.HexColor("#D7CCC8"))
        self.setLineWidth(1)
        self.line(36, 42, letter[0] - 36, 42)

        # Footer Text
        self.setFont("Helvetica", 8)
        self.setFillColor(colors.HexColor("#8D6E63"))
        self.drawString(36, 28, "Samsung Galaxy S24 Ultra Edition • Android 14+")
        self.drawRightString(letter[0] - 36, 28, f"Page {self._pageNumber} of {page_count}")
        self.restoreState()

def build_pdf(filename):
    doc = SimpleDocTemplate(
        filename,
        pagesize=letter,
        leftMargin=36,
        rightMargin=36,
        topMargin=48,
        bottomMargin=54
    )

    styles = getSampleStyleSheet()

    # Custom styles
    title_style = ParagraphStyle(
        'DocTitle',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=24,
        leading=28,
        textColor=colors.HexColor("#D84315"),
        alignment=1, # Center
        spaceAfter=4
    )

    subtitle_style = ParagraphStyle(
        'DocSubtitle',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=12,
        leading=16,
        textColor=colors.HexColor("#5D4037"),
        alignment=1,
        spaceAfter=14
    )

    h1_style = ParagraphStyle(
        'SectionH1',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=13,
        leading=16,
        textColor=colors.HexColor("#BF360C"),
        spaceBefore=10,
        spaceAfter=6,
        keepWithNext=True
    )

    body_style = ParagraphStyle(
        'BodyDark',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=9.5,
        leading=13.5,
        textColor=colors.HexColor("#2E1C14"),
        spaceAfter=6
    )

    bullet_style = ParagraphStyle(
        'BulletText',
        parent=body_style,
        leftIndent=15,
        firstLineIndent=-10,
        spaceAfter=4
    )

    callout_style = ParagraphStyle(
        'CalloutText',
        parent=styles['Normal'],
        fontName='Helvetica-Oblique',
        fontSize=9,
        leading=13,
        textColor=colors.HexColor("#3E2723")
    )

    story = []

    # Title Banner
    story.append(Paragraph("🐾 HERBERT'S ZOOMIES 🐾", title_style))
    story.append(Paragraph("Player Instruction Manual & Gameplay Guide", subtitle_style))
    story.append(HRFlowable(width="100%", thickness=1.5, color=colors.HexColor("#FF7043"), spaceAfter=10))

    # Section: Game Concept
    story.append(Paragraph("1. GAME OVERVIEW & STORY", h1_style))
    story.append(Paragraph(
        "<b>Herbert's Zoomies</b> is an energetic, arcade runner starring <b>Herbert</b>, a real Snowshoe Siamese kitten. "
        "Herbert has suddenly lost his tiny kitten mind and is tearing through the living room at absurd speeds! "
        "Your goal is to guide Herbert through the house, steer around household furniture, leap over hurdles, "
        "collect mouth-watering treats and toys, navigate his grumpy older housemate <b>Kitty</b>, and unleash the legendary <b>Maximum Zoomies</b> mode!",
        body_style
    ))

    # Visual Screenshot Embedding
    screen_path = "/home/draygen/.gemini/antigravity-cli/brain/6982a806-d48d-450f-959f-a69838a063af/screen_kitty_loaf.png"
    if os.path.exists(screen_path):
        story.append(Spacer(1, 4))
        # Keep aspect ratio roughly 2:1 width:height
        img = Image(screen_path, width=480, height=215)
        story.append(img)
        story.append(Paragraph("<i>Figure 1: Herbert activating Maximum Zoomies while leaping past Kitty and collecting toys.</i>", callout_style))
        story.append(Spacer(1, 8))

    # Section: Controls & Maneuvers
    story.append(Paragraph("2. TOUCH CONTROLS & HOW TO PLAY", h1_style))
    controls_data = [
        [Paragraph("<b>Action</b>", body_style), Paragraph("<b>Touch Gesture</b>", body_style), Paragraph("<b>Gameplay Effect</b>", body_style)],
        [
            Paragraph("<b>Steer / Lane Drift</b>", body_style),
            Paragraph("Drag finger UP / DOWN on screen", body_style),
            Paragraph("Smoothly navigates Herbert across living room lanes with responsive kitten tilts.", body_style)
        ],
        [
            Paragraph("<b>Springy Jump / Pounce</b>", body_style),
            Paragraph("Tap Screen OR Swipe UP", body_style),
            Paragraph("Herbert leaps into the air with generous hang-time to clear slippers, tunnels, cushions, and Kitty!", body_style)
        ],
        [
            Paragraph("<b>Menu Selection</b>", body_style),
            Paragraph("Tap \"PLAY\" or \"AGAIN!\"", body_style),
            Paragraph("Starts a new run immediately without loading screens or ads.", body_style)
        ]
    ]
    t_controls = Table(controls_data, colWidths=[120, 150, 270])
    t_controls.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor("#FFE0B2")),
        ('GRID', (0, 0), (-1, -1), 0.5, colors.HexColor("#BCAAA4")),
        ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
        ('TOPPADDING', (0, 0), (-1, -1), 4),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 4),
    ]))
    story.append(t_controls)
    story.append(Spacer(1, 8))

    # Page Break for Clean 2-Page Layout
    story.append(PageBreak())

    # Section: Cast of Characters
    story.append(Paragraph("3. CHARACTERS", h1_style))
    char_data = [
        [
            Paragraph("<b>HERBERT (The Kitten)</b>", body_style),
            Paragraph("• <b>Appearance:</b> Pure cream-white fur, dark seal brown ear points, dark eye mask with a white inverted-V blaze, bubblegum pink nose & beans.<br/>"
                      "• <b>Personality:</b> Hyperactive, curious, innocent, springy.<br/>"
                      "• <b>Abilities:</b> High jumping parabolic arc, rapid steering, Maximum Zoomie invulnerability.", body_style)
        ],
        [
            Paragraph("<b>KITTY (The 9yo Matriarch)</b>", body_style),
            Paragraph("• <b>Appearance:</b> Compact, stocky brown/gray mackerel tabby with forehead \"M\" stripes and unamused gold-green eyes.<br/>"
                      "• <b>Personality:</b> Calm, grumpy, completely unimpressed by Herbert's chaos.<br/>"
                      "• <b>Behaviors:</b> Loafing in lanes, taking midday catnaps (with Zzz floaties), slow waddling, and swatting with cartoon claws if Herbert rushes too close.", body_style)
        ]
    ]
    t_chars = Table(char_data, colWidths=[160, 380])
    t_chars.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, -1), colors.HexColor("#FFF8E1")),
        ('GRID', (0, 0), (-1, -1), 0.5, colors.HexColor("#FFE082")),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
        ('TOPPADDING', (0, 0), (-1, -1), 6),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 6),
    ]))
    story.append(t_chars)
    story.append(Spacer(1, 10))

    # Section: Pickups, Scoring & Zoomies
    story.append(Paragraph("4. PICKUPS, COMBO SYSTEM & MAXIMUM ZOOMIES", h1_style))
    items_data = [
        [Paragraph("<b>Item / Event</b>", body_style), Paragraph("<b>Base Points</b>", body_style), Paragraph("<b>Zoomie Energy</b>", body_style), Paragraph("<b>Effect</b>", body_style)],
        [Paragraph("🐟 <b>Fish Treat</b>", body_style), Paragraph("+100 pts", body_style), Paragraph("+25%", body_style), Paragraph("Tasty snack that rapidly charges Zoomie Energy.", body_style)],
        [Paragraph("🐭 <b>Toy Mouse</b>", body_style), Paragraph("+250 pts", body_style), Paragraph("+35%", body_style), Paragraph("Plush toy mouse with bouncy spring tail.", body_style)],
        [Paragraph("🧶 <b>Yarn Ball</b>", body_style), Paragraph("+250 pts", body_style), Paragraph("+35%", body_style), Paragraph("Vibrant wool ball with trailing yarn ribbon.", body_style)],
        [Paragraph("✨ <b>Near Miss</b>", body_style), Paragraph("+150 pts", body_style), Paragraph("+20%", body_style), Paragraph("Skimming close to an obstacle without touching.", body_style)],
    ]
    t_items = Table(items_data, colWidths=[110, 80, 95, 255])
    t_items.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor("#E0F7FA")),
        ('GRID', (0, 0), (-1, -1), 0.5, colors.HexColor("#80DEEA")),
        ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
        ('TOPPADDING', (0, 0), (-1, -1), 4),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 4),
    ]))
    story.append(t_items)
    story.append(Spacer(1, 8))

    story.append(Paragraph(
        "<b>⚡ MAXIMUM ZOOMIES:</b> When the Zoomie Energy meter hits 100%, Herbert enters <b>Maximum Zoomies mode</b> for 6 seconds! "
        "His pupils dilate huge, speed increases by +35%, all score points are <b>TRIPLED (3x Multiplier)</b>, "
        "and soft obstacles harmlessly bounce out of his path!",
        body_style
    ))

    # Section: Forgiving Failure & Health Buffer
    story.append(Paragraph("5. STUMBLE BUFFER & COZY PHILOSOPHY", h1_style))
    story.append(Paragraph(
        "• <b>Life Buffer (Stumble Recovery):</b> Herbert has a built-in mistake buffer. The first time he bumps into a hazard, "
        "he experiences a comical <b>Stumble</b> with 1.5 seconds of invulnerability flicker and temporary speed slowdown, rather than an instant defeat.<br/>"
        "• <b>The Flop:</b> If a second collision occurs, Herbert tires out and flops onto his pink belly for a nap. "
        "There is zero violence or harsh penalty—simply tap <b>\"AGAIN!\"</b> to start a fresh run!",
        body_style
    ))

    # Section: Technical Specs
    story.append(Spacer(1, 4))
    story.append(Paragraph("6. TECHNICAL & BUILD SPECIFICATIONS", h1_style))
    story.append(Paragraph(
        "• <b>Engine:</b> Custom Kotlin 2D SurfaceView Vector Simulation (1080x2340 Fullscreen Fill)<br/>"
        "• <b>Target Platform:</b> Samsung Galaxy S24 Ultra (`SM-S928U`), Android 14+ (SDK 35)<br/>"
        "• <b>Audio & Privacy:</b> 100% synthesized PCM audio, zero external analytics, zero ads, zero internet permissions required.",
        body_style
    ))

    doc.build(story, canvasmaker=NumberedCanvas)
    print(f"Manual built successfully at: {filename}")

if __name__ == '__main__':
    destinations = [
        "/mnt/c/Users/Administrator/Desktop/Herberts_Zoomies_Instruction_Manual.pdf",
        "/mnt/c/Users/Administrator/Documents/Herberts_Zoomies_Instruction_Manual.pdf",
        "/mnt/c/Users/Administrator/Downloads/Herberts_Zoomies_Instruction_Manual.pdf",
        "/mnt/c/Users/draygen.DESKTOP-21V20RE/Desktop/Herberts_Zoomies_Instruction_Manual.pdf",
        "/mnt/c/Users/draygen.DESKTOP-21V20RE/Documents/Herberts_Zoomies_Instruction_Manual.pdf",
        "/mnt/c/Users/draygen.DESKTOP-21V20RE/Downloads/Herberts_Zoomies_Instruction_Manual.pdf",
        "/home/draygen/herbertzoom/docs/Herberts_Zoomies_Instruction_Manual.pdf"
    ]

    os.makedirs("/home/draygen/herbertzoom/docs", exist_ok=True)
    temp_pdf = "/tmp/Herberts_Zoomies_Instruction_Manual.pdf"
    build_pdf(temp_pdf)

    import shutil
    for d in destinations:
        try:
            target_dir = os.path.dirname(d)
            if os.path.exists(target_dir):
                shutil.copy(temp_pdf, d)
                print(f"Copied to: {d}")
        except Exception as e:
            print(f"Could not copy to {d}: {e}")
