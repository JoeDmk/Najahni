"""
Migrate Dali's Community module into the unified Najahni project.
Transforms package declarations, imports, DB connections, and FXML paths.
"""
import os
import re
import shutil

# Paths
SRC = r"C:\Users\ilyes\Downloads\najahni\Community\src\main\java"
RES = r"C:\Users\ilyes\Downloads\najahni\Community\src\main\resources"
DST_JAVA = r"C:\Users\ilyes\Downloads\najahni\PIDEV-3A-JAVA-Gestion-Users\src\main\java"
DST_RES = r"C:\Users\ilyes\Downloads\najahni\PIDEV-3A-JAVA-Gestion-Users\src\main\resources"

# Package mappings (old → new)
PACKAGE_MAP = {
    "Entites": "models.community",
    "Services": "services.community",
    "Controller": "controllers.community",
    "Utils": "tools",
    "GUI": "test",  # Main class - skipped separately
    "Tests": "test",
}

# Folder mappings (source subfolder → destination subfolder under DST_JAVA)
FOLDER_MAP = {
    "Entites": "models/community",
    "Services": "services/community",
    "Controller": "controllers/community",
}

# Files to skip (we don't need Dali's main class, DB connection, or test runners)
SKIP_FILES = {
    "MyBD.java",           # We use tools.MyConnection
    "MainConnection.java", # Dali's GUI main class
    "HomePage.java",       # Dali's GUI main class
}

# FXML path transformations (old resource path → new)
FXML_RENAMES = {
    "/CommunityHomePage.fxml": "/views/community/CommunityHomePage.fxml",
    "/GroupsPage.fxml": "/views/community/GroupsPage.fxml",
    "/GroupDashboard.fxml": "/views/community/GroupDashboard.fxml",
    "/AddGroup.fxml": "/views/community/AddGroup.fxml",
    "/AddThread.fxml": "/views/community/AddThread.fxml",
    "/EditThread.fxml": "/views/community/EditThread.fxml",
    "/ThreadPage.fxml": "/views/community/ThreadPage.fxml",
    "/PostsPage.fxml": "/views/community/PostsPage.fxml",
    "/EventsPage.fxml": "/views/community/EventsPage.fxml",
    "/EventDetailsPage.fxml": "/views/community/EventDetailsPage.fxml",
    "/AddEvent.fxml": "/views/community/AddEvent.fxml",
    "/styles/app.css": "/views/community/styles/app.css",
}


def transform_java(content: str, src_package: str) -> str:
    """Apply all transformations to a Java source file."""
    
    # 1. Package declaration
    new_pkg = PACKAGE_MAP.get(src_package, src_package)
    content = re.sub(
        r'^(\s*)package\s+' + re.escape(src_package) + r'\s*;',
        r'\1package ' + new_pkg + ';',
        content,
        flags=re.MULTILINE
    )
    
    # 2. Import transformations
    # import Entites.XXX → import models.community.XXX
    content = re.sub(r'import\s+Entites\.', 'import models.community.', content)
    # import Entites.* → import models.community.*
    content = content.replace('import Entites.*;', 'import models.community.*;')
    
    # import Services.XXX → import services.community.XXX
    content = re.sub(r'import\s+Services\.', 'import services.community.', content)
    # import Services.* → import services.community.*
    content = content.replace('import Services.*;', 'import services.community.*;')
    
    # import Controller.XXX → import controllers.community.XXX
    content = re.sub(r'import\s+Controller\.', 'import controllers.community.', content)
    
    # import Utils.MyBD → import tools.MyConnection
    content = content.replace('import Utils.MyBD;', 'import tools.MyConnection;')
    content = re.sub(r'import\s+Utils\.', 'import tools.', content)
    
    # 3. DB connection: MyBD.getInstance().getConn() → MyConnection.getInstance().getConnection()
    content = content.replace('MyBD.getInstance().getConn()', 'MyConnection.getInstance().getConnection()')
    content = content.replace('MyBD.getInstance()', 'MyConnection.getInstance()')
    # Also handle any remaining direct MyBD references
    content = content.replace('MyBD', 'MyConnection')
    
    # 4. Fully qualified references: Services.XXX → services.community.XXX
    content = re.sub(r'\bServices\.WeatherService\b', 'services.community.WeatherService', content)
    content = re.sub(r'\bServices\.EmailAsync\b', 'services.community.EmailAsync', content)
    content = re.sub(r'\bServices\.EventCRUD\b', 'services.community.EventCRUD', content)
    content = re.sub(r'\bServices\.NotificationEmailService\b', 'services.community.NotificationEmailService', content)
    content = re.sub(r'\bServices\.QrDecodeUtil\b', 'services.community.QrDecodeUtil', content)
    content = re.sub(r'\bServices\.QrUtil\b', 'services.community.QrUtil', content)
    content = re.sub(r'\bServices\.TicketSigner\b', 'services.community.TicketSigner', content)
    
    # 5. IntrefaceCRUD import: since it's inside services.community, we add the import
    # Only for files implementing IntrefaceCRUD
    if 'implements IntrefaceCRUD' in content:
        # Check if import already present
        if 'import services.community.IntrefaceCRUD' not in content and 'import interfaces.community.IntrefaceCRUD' not in content:
            # IntrefaceCRUD is in services.community already - same package, no import needed if same package
            pass
    
    # 6. FXML paths
    for old_path, new_path in FXML_RENAMES.items():
        content = content.replace(f'getResource("{old_path}")', f'getResource("{new_path}")')
    
    return content


def migrate_java_files():
    """Migrate all Java source files."""
    processed = []
    
    for src_pkg, dst_folder in FOLDER_MAP.items():
        src_dir = os.path.join(SRC, src_pkg)
        dst_dir = os.path.join(DST_JAVA, dst_folder)
        
        if not os.path.exists(src_dir):
            print(f"  SKIP (not found): {src_dir}")
            continue
        
        os.makedirs(dst_dir, exist_ok=True)
        
        for filename in os.listdir(src_dir):
            if not filename.endswith('.java'):
                continue
            if filename in SKIP_FILES:
                print(f"  SKIP: {filename}")
                continue
            
            src_file = os.path.join(src_dir, filename)
            dst_file = os.path.join(dst_dir, filename)
            
            with open(src_file, 'r', encoding='utf-8') as f:
                content = f.read()
            
            content = transform_java(content, src_pkg)
            
            with open(dst_file, 'w', encoding='utf-8') as f:
                f.write(content)
            
            processed.append(f"  {src_pkg}/{filename} → {dst_folder}/{filename}")
    
    return processed


def migrate_interface():
    """Migrate IntrefaceCRUD to services.community (it's used as the CRUD interface)."""
    src_file = os.path.join(SRC, "Services", "IntrefaceCRUD.java")
    dst_dir = os.path.join(DST_JAVA, "interfaces", "community")
    os.makedirs(dst_dir, exist_ok=True)
    dst_file = os.path.join(dst_dir, "IntrefaceCRUD.java")
    
    if os.path.exists(src_file):
        with open(src_file, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # Change package to interfaces.community
        content = content.replace('package Services;', 'package interfaces.community;')
        
        with open(dst_file, 'w', encoding='utf-8') as f:
            f.write(content)
        
        print(f"  Services/IntrefaceCRUD.java → interfaces/community/IntrefaceCRUD.java")
        
        # Now we need to add import of interfaces.community.IntrefaceCRUD to all services that use it
        return True
    return False


def fix_interface_imports():
    """Add import for IntrefaceCRUD in service files that implement it."""
    services_dir = os.path.join(DST_JAVA, "services", "community")
    
    for filename in os.listdir(services_dir):
        if not filename.endswith('.java'):
            continue
        
        filepath = os.path.join(services_dir, filename)
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        if 'implements IntrefaceCRUD' in content:
            # Add import if not already present
            if 'import interfaces.community.IntrefaceCRUD' not in content:
                # Add import after the package declaration
                content = re.sub(
                    r'(package services\.community;)',
                    r'\1\n\nimport interfaces.community.IntrefaceCRUD;',
                    content,
                    count=1
                )
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(content)
                print(f"  Added IntrefaceCRUD import to {filename}")


def migrate_fxml():
    """Copy FXML files to views/community/."""
    dst_fxml = os.path.join(DST_RES, "views", "community")
    os.makedirs(dst_fxml, exist_ok=True)
    
    # Dali's FXML files are in resources/ directly or resources/views/
    # Let me check both locations
    fxml_dirs = [
        RES,
        os.path.join(RES, "views"),
    ]
    
    copied = []
    for fxml_dir in fxml_dirs:
        if not os.path.exists(fxml_dir):
            continue
        for filename in os.listdir(fxml_dir):
            if filename.endswith('.fxml'):
                src = os.path.join(fxml_dir, filename)
                dst = os.path.join(dst_fxml, filename)
                
                # Read and transform controller references in FXML
                with open(src, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                # Fix controller class references in FXML
                content = content.replace('Controller.', 'controllers.community.')
                
                # Fix any CSS references
                content = content.replace('@styles/app.css', '@../views/community/styles/app.css')
                
                with open(dst, 'w', encoding='utf-8') as f:
                    f.write(content)
                
                copied.append(f"  {filename} → views/community/{filename}")
    
    return copied


def migrate_css_and_images():
    """Copy CSS and images."""
    copied = []
    
    # styles directory
    styles_src = os.path.join(RES, "styles")
    styles_dst = os.path.join(DST_RES, "views", "community", "styles")
    if os.path.exists(styles_src):
        os.makedirs(styles_dst, exist_ok=True)
        for filename in os.listdir(styles_src):
            src = os.path.join(styles_src, filename)
            dst = os.path.join(styles_dst, filename)
            if os.path.isfile(src):
                shutil.copy2(src, dst)
                copied.append(f"  styles/{filename}")
    
    # images directory
    images_src = os.path.join(RES, "images")
    images_dst = os.path.join(DST_RES, "views", "community", "images")
    if os.path.exists(images_src):
        os.makedirs(images_dst, exist_ok=True)
        for filename in os.listdir(images_src):
            src = os.path.join(images_src, filename)
            dst = os.path.join(images_dst, filename)
            if os.path.isfile(src):
                shutil.copy2(src, dst)
                copied.append(f"  images/{filename}")
    
    return copied


def main():
    print("=" * 60)
    print("COMMUNITY MODULE MIGRATION")
    print("=" * 60)
    
    print("\n1. Migrating Java source files...")
    files = migrate_java_files()
    for f in files:
        print(f)
    print(f"   Total: {len(files)} files")
    
    print("\n2. Migrating IntrefaceCRUD interface...")
    migrate_interface()
    
    print("\n3. Fixing IntrefaceCRUD imports in services...")
    fix_interface_imports()
    
    print("\n4. Migrating FXML files...")
    fxml = migrate_fxml()
    for f in fxml:
        print(f)
    print(f"   Total: {len(fxml)} FXML files")
    
    print("\n5. Migrating CSS and images...")
    assets = migrate_css_and_images()
    for a in assets:
        print(a)
    print(f"   Total: {len(assets)} asset files")
    
    print("\n" + "=" * 60)
    print("MIGRATION COMPLETE!")
    print("=" * 60)
    print("\nNext steps:")
    print("  1. Run: mvn clean compile")
    print("  2. Fix any remaining compilation errors")


if __name__ == "__main__":
    main()
