import sys
import os
from reportlab.lib.pagesizes import letter
from reportlab.lib import colors
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, KeepTogether, HRFlowable
)
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
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
            self.draw_page_number(num_pages)
            canvas.Canvas.showPage(self)
        canvas.Canvas.save(self)

    def draw_page_number(self, page_count):
        self.saveState()
        self.setFont("Helvetica-Bold", 8)
        self.setFillColor(colors.HexColor("#64748B"))
        
        # Header (pages > 1)
        if self._pageNumber > 1:
            self.drawString(54, 750, "ROAM (SIH26204/26202) — Internal Hackathon Speech & Jury Defense Guide")
            self.setStrokeColor(colors.HexColor("#CBD5E1"))
            self.setLineWidth(0.5)
            self.line(54, 742, 558, 742)
            
        # Footer
        page_text = f"Page {self._pageNumber} of {page_count}"
        self.drawRightString(558, 36, page_text)
        self.drawString(54, 36, "TEAM ALGOHUNTERS — SMART INDIA HACKATHON 2026 INTERNAL SELECTION")
        self.setStrokeColor(colors.HexColor("#CBD5E1"))
        self.setLineWidth(0.5)
        self.line(54, 48, 558, 48)
        self.restoreState()

def build_pdf():
    pdf_filename = "ROAM_SIH2026_Internal_Hackathon_Speech_and_Defense_Guide.pdf"
    doc = SimpleDocTemplate(
        pdf_filename,
        pagesize=letter,
        leftMargin=54,
        rightMargin=54,
        topMargin=54,
        bottomMargin=54
    )

    styles = getSampleStyleSheet()
    
    # Custom styles
    primary_color = colors.HexColor("#0F172A")    # Obsidian Dark
    gold_color = colors.HexColor("#D4AF37")       # Metallic Gold
    accent_blue = colors.HexColor("#0284C7")      # Sky Blue
    text_dark = colors.HexColor("#1E293B")        # Slate Dark
    bg_subtle = colors.HexColor("#F8FAFC")        # Soft Slate

    title_style = ParagraphStyle(
        'DocTitle',
        parent=styles['Heading1'],
        fontName='Helvetica-Bold',
        fontSize=22,
        leading=26,
        textColor=primary_color,
        spaceAfter=4
    )

    subtitle_style = ParagraphStyle(
        'DocSubtitle',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=11,
        leading=15,
        textColor=accent_blue,
        spaceAfter=12
    )

    h1_style = ParagraphStyle(
        'SectionH1',
        parent=styles['Heading2'],
        fontName='Helvetica-Bold',
        fontSize=14,
        leading=18,
        textColor=primary_color,
        spaceBefore=14,
        spaceAfter=8,
        keepWithNext=True
    )

    h2_style = ParagraphStyle(
        'SectionH2',
        parent=styles['Heading3'],
        fontName='Helvetica-Bold',
        fontSize=11.5,
        leading=15,
        textColor=gold_color,
        spaceBefore=10,
        spaceAfter=4,
        keepWithNext=True
    )

    body_style = ParagraphStyle(
        'BodyDark',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=9.5,
        leading=13.5,
        textColor=text_dark,
        spaceAfter=6
    )

    quote_style = ParagraphStyle(
        'SpeechQuote',
        parent=styles['Normal'],
        fontName='Helvetica-Oblique',
        fontSize=9.5,
        leading=14,
        textColor=colors.HexColor("#0F172A"),
        backColor=colors.HexColor("#F1F5F9"),
        borderColor=colors.HexColor("#0284C7"),
        borderWidth=1,
        borderPadding=10,
        spaceBefore=6,
        spaceAfter=10
    )

    bullet_style = ParagraphStyle(
        'BulletDark',
        parent=body_style,
        leftIndent=12,
        firstLineIndent=-8,
        spaceAfter=4
    )

    table_cell_style = ParagraphStyle(
        'TableCell',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=8.5,
        leading=11.5,
        textColor=text_dark
    )

    table_header_style = ParagraphStyle(
        'TableHeader',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=9,
        leading=12,
        textColor=colors.white
    )

    story = []

    # Title Block
    story.append(Paragraph("🏛️ ROAM — Internal SIH Selection Presentation & Pitch Dossier", title_style))
    story.append(Paragraph("<b>Team Name: AlgoHunters</b> | Problem Statement: Boosting Tourism, Hotels & Travel (SIH26204/SIH26202) | Category: Software", subtitle_style))
    story.append(HRFlowable(width="100%", thickness=2, color=gold_color, spaceAfter=12))

    # Executive Overview
    overview_html = """
    <b>PROJECT BRIEF:</b><br/>
    <b>ROAM</b> (<i>Real-time Odyssey for Augmented Monuments</i>) / <b>MonumentQuest</b> is an AR-powered heritage exploration app fused with a direct hotel-booking economy. Built on native Android (Kotlin, Jetpack Compose), Node.js TypeScript API, PostgreSQL/Prisma, OpenStreetMap Overpass live geocoding, and Groq AI, ROAM lifts off-season hotel occupancy while redirecting footfall to over 500 under-visited cultural landmarks.
    """
    story.append(Paragraph(overview_html, body_style))
    story.append(Spacer(1, 8))

    # SECTION 1: 3-MINUTE RAPID ELEVATOR PITCH
    story.append(Paragraph("1. ⏱️ 3-Minute Elevator Pitch (Standard SIH Selection Limit)", h1_style))
    pitch_3min = """
    "Good morning, respected judges and faculty evaluators. We are Team <b>AlgoHunters</b>, presenting <b>ROAM</b> — <i>Real-time Odyssey for Augmented Monuments</i>.<br/><br/>
    Across India, 80% of tourist footfall is trapped in just a handful of major landmarks, leaving 500+ historic shrines and local artisans completely unvisited. At the same time, regional hotels suffer from off-season occupancy slumps as low as 35%, while losing up to 25% of their revenue to online booking aggregators. The travel ecosystem needs an innovation that connects tourists, heritage, and local hospitality into one rewarding experience.<br/><br/>
    Our solution, <b>ROAM</b>, is a gamified AR heritage platform fused with a direct hotel-booking economy.<br/><br/>
    When a traveler visits a historic city like Bhubaneswar, ROAM streams their position at 5Hz using a Google Fused Location Engine and Kalman velocity filter. Using live OpenStreetMap Overpass queries, the app triggers a 180m AR Proximity Radar pulse. Explorers scan monuments using CameraX AR overlays, earning up to 5x XP multipliers for first-time discoveries, which unlocks digital passport entries in their Pokédex-style collection vault.<br/><br/>
    To boost the local travel economy, ROAM introduces the <b>SIH Partner Hotel Quest Pass</b>. When near a monument, the app surfaces verified nearby stays where tourists claim QR vouchers offering 15 to 20% discounts and 2x XP multipliers on check-in. This fills vacant hotel rooms, eliminates heavy aggregator commissions, and keeps tourism spending local.<br/><br/>
    Technically, we aren't presenting just a concept — <b>ROAM has a working Android prototype and production backend</b>. Built natively in Kotlin using Jetpack Compose, Material 3, Hilt Dependency Injection, and ARCore on mobile; Node.js, Express, TypeScript, PostgreSQL, and Prisma ORM on backend; integrated with Groq AI for storytelling, and featuring Ghost Mode for privacy and Journalist Mode for off-season retention.<br/><br/>
    The business impact is quantifiable: a <b>+28% surge in off-season hotel occupancy</b>, 15 to 20% higher direct booking margins for hoteliers, and a <b>+35% increase in footfall to micro-heritage zones</b>.<br/><br/>
    ROAM transforms static sightseeing into an active quest while solving real economic bottlenecks for hotels and travel logistics. We are Team AlgoHunters, ready to represent our institution at Smart India Hackathon 2026. Thank you!"
    """
    story.append(Paragraph(pitch_3min, quote_style))
    story.append(Spacer(1, 10))

    # SECTION 2: SLIDE-BY-SLIDE PRESENTATION SCRIPT (ROAM.PPT2-3.pptx)
    story.append(Paragraph("2. 📽️ Slide-by-Slide Delivery & Speaker Notes (ROAM.PPT2-3.pptx)", h1_style))

    slides_data = [
        ("Slide 1: Title & Introduction", 
         "<b>Slide Header:</b> SMART INDIA HACKATHON 2026 — Team AlgoHunters (ROAM)<br/>"
         "<b>Speaker Script:</b> <i>'Respected evaluators, we are Team AlgoHunters. Today we are presenting ROAM, an Augmented Reality platform designed to revolutionize tourism, hotel logistics, and heritage exploration under the Smart India Hackathon Travel & Tourism domain.'</i>"),
        
        ("Slide 2: Proposed Solution", 
         "<b>Slide Header:</b> PROPOSED SOLUTION — Gamified AR App & Hotel Quest Pass<br/>"
         "<b>Speaker Script:</b> <i>'Current tourism platforms are static directories that fail to engage digital-first travelers or help local hotels during off-seasons. ROAM bridges this gap by fusing AR heritage exploration with an incentivized hotel-booking economy. Through camera-based monument discovery, an AI Cultural Historian, and QR-based Partner Hotel Passes, we lift hotel occupancy while redirecting footfall to over 500 under-visited heritage sites.'</i>"),
        
        ("Slide 3: Technical Approach", 
         "<b>Slide Header:</b> TECHNICAL APPROACH — Native Android, 5Hz GPS, Node.js & Prisma<br/>"
         "<b>Speaker Script:</b> <i>'Our technical architecture is built for real-world scale: On mobile, we use Kotlin Jetpack Compose with MVVM Clean Architecture, paired with a 5Hz Fused Location Client and Kalman filter for continuous location streaming. For mapping, we query the OpenStreetMap Overpass API live, keeping infrastructure costs low. The backend runs on Node.js, Express, and PostgreSQL via Prisma ORM, handling spatial geofencing, rate-limiting, and instant QR voucher validation.'</i>"),
        
        ("Slide 4: Feasibility & Risk Mitigation", 
         "<b>Slide Header:</b> FEASIBILITY AND VIABILITY — Working Prototype & Odisha Pilot<br/>"
         "<b>Speaker Script:</b> <i>'ROAM is highly feasible because we already have a fully compiled, working Android app and live API backend. We are launching with a curated pilot loop in Bhubaneswar before expanding nationally. To address risks like GPS spoofing or AI hallucinations, we enforce strict 100m geofence checks, server-side validation, and curated historical prompt grounding. User privacy is protected via opt-in Ghost Mode location obfuscation.'</i>"),
        
        ("Slide 5: Impact & Business Benefits", 
         "<b>Slide Header:</b> IMPACT AND BENEFITS — Direct ROI for Hotels & Tourism<br/>"
         "<b>Speaker Script:</b> <i>'ROAM delivers measurable ROI across all tourism stakeholders: Hotels gain direct bookings without paying 18–25% OTA commissions, increasing direct margins by up to 20%. Drives a +35% footfall increase to secondary heritage shrines, helping local artisans and food vendors. Tourists save up to 20% on stays while earning XP, and tourism boards get anonymized visitor heatmaps for infrastructure planning.'</i>"),
        
        ("Slide 6: Research & References", 
         "<b>Slide Header:</b> RESEARCH AND REFERENCES — GitHub & Industry Benchmarks<br/>"
         "<b>Speaker Script:</b> <i>'Our solution is grounded in rigorous research, real hotel commission data, and OpenStreetMap spatial data. Our complete code is structured and hosted on GitHub. We are confident ROAM will be a standout entry at SIH 2026. Thank you, and we welcome your questions!'</i>")
    ]

    for title, content in slides_data:
        story.append(Paragraph(title, h2_style))
        story.append(Paragraph(content, body_style))
        story.append(Spacer(1, 4))

    story.append(Spacer(1, 10))

    # SECTION 3: JURY Q&A DEFENSE STRATEGY
    story.append(Paragraph("3. 🎯 Jury Q&A Defense Strategy (Internal Evaluation)", h1_style))

    qa_data = [
        [Paragraph("Expected Jury Question", table_header_style), Paragraph("Fact-Checked Strategic Answer", table_header_style)],
        
        [Paragraph("<b>Q1: How is ROAM different from Google Maps or TripAdvisor?</b>", table_cell_style),
         Paragraph("Google Maps and TripAdvisor are passive search directories that take users out of the physical experience and charge hotels high commission fees. ROAM is an <b>active gamified quest ecosystem</b>. It uses 5Hz location streaming, AR proximity scanning, AI storytelling, and dynamic QR Quest Passes that connect monument visitors directly to nearby hotels with zero-to-low commission margins.", table_cell_style)],
        
        [Paragraph("<b>Q2: How do you prevent users from spoofing GPS to farm XP & hotel passes?</b>", table_cell_style),
         Paragraph("We enforce server-side spatial validation in Node.js. Every discovery check-in requires a <b>100-meter spatial geofence check</b> against live coordinates, velocity validation via Kalman filtering, and rate-limiting rules. Quest Pass QR codes expire in 24 hours and must be scanned physically at the hotel front desk.", table_cell_style)],
        
        [Paragraph("<b>Q3: How do you handle AI hallucinations in heritage history?</b>", table_cell_style),
         Paragraph("Our AI Cultural Historian (powered by Groq) uses strict system prompts grounded in verified architectural datasets (Kalinga & Indo-Islamic heritage records). For critical historical facts, it falls back to curated local metadata.", table_cell_style)],
        
        [Paragraph("<b>Q4: What is your business model to make this self-sustainable?</b>", table_cell_style),
         Paragraph("We operate on three monetization streams:<br/>1. <b>B2B Hotel Subscriptions:</b> Tiered listing fees for partner hotels in Tourist Quest Pass.<br/>2. <b>Sponsored Guild Expeditions:</b> Regional tourism boards sponsor guild challenges.<br/>3. <b>Low Pass Commissions:</b> Nominal 3–5% transaction fee on claimed passes (80% cheaper than OTAs).", table_cell_style)]
    ]

    t_qa = Table(qa_data, colWidths=[170, 334])
    t_qa.setStyle(TableStyle([
        ('BACKGROUND', (0,0), (-1,0), primary_color),
        ('ALIGN', (0,0), (-1,-1), 'LEFT'),
        ('VALIGN', (0,0), (-1,-1), 'TOP'),
        ('GRID', (0,0), (-1,-1), 0.5, colors.HexColor("#CBD5E1")),
        ('ROWBACKGROUNDS', (0,1), (-1,-1), [colors.white, bg_subtle]),
        ('TOPPADDING', (0,0), (-1,-1), 6),
        ('BOTTOMPADDING', (0,0), (-1,-1), 6),
    ]))
    story.append(t_qa)

    doc.build(story, canvasmaker=NumberedCanvas)
    print(f"PDF generated successfully: {pdf_filename}")

if __name__ == '__main__':
    build_pdf()
