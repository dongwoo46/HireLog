# scripts/test_parser_logic.py
import sys
import os

sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "src")))

from url.parser import UrlParser

def test_parser():
    print("==========================================")
    print(" TEST: UrlParser Noise Removal")
    print("==========================================")
    
    parser = UrlParser()
    
    mock_html = """
    <html>
        <head><title>Test Position - Naver Webtoon</title></head>
        <body>
            <header class="nav">
                <div class="menu">Home</div>
                <div class="login">Login</div>
                <div class="gnb">Menu items...</div>
            </header>
            
            <div id="content">
                <h1>ML Platform Engineer</h1>
                <div class="job-description">
                    <h2>Responsibilities</h2>
                    <p>Build large scale ML platforms.</p>
                    <p>Optimize GPU infrastructure.</p>
                    
                    <h2>Requirements</h2>
                    <ul>
                        <li>Experience with Kubernetes.</li>
                        <li>Proficiency in Python.</li>
                    </ul>
                </div>
            </div>
            
            <footer>
                <div class="footer-links">Terms | Privacy</div>
                <div class="share-buttons">Share on Facebook</div>
                <button>Apply Now</button>
            </footer>
            
            <div class="cookie-banner">Accept Cookies</div>
            <div class="popup">Subscribe to newsletter</div>
        </body>
    </html>
    """
    
    parsed = parser.parse(mock_html)
    
    print(f"Title: {parsed['title']}")
    print("\n[Body Content Start]")
    print(parsed["body"])
    print("[Body Content End]\n")
    
    # Assertions
    noise_keywords = ["Home", "Login", "Terms", "Privacy", "Share", "Accept Cookies", "Subscribe"]
    
    failed = []
    for kw in noise_keywords:
        if kw in parsed["body"]:
            failed.append(kw)
            
    if failed:
        print(f"[FAIL] Check failed. Noise found: {failed}")
    else:
        print("[PASS] All noise keywords removed.")
        
    expected_content = ["ML Platform Engineer", "Responsibilities", "Build large scale", "Requirements", "Kubernetes"]
    missing = []
    for content in expected_content:
        if content not in parsed["body"]:
            missing.append(content)
            
    if missing:
        print(f"[FAIL] Check failed. Missing content: {missing}")
    else:
        print("[PASS] All expected content found.")

if __name__ == "__main__":
    test_parser()
