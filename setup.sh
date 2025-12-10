#!/bin/bash

# Home Server Setup Script
# This script clones and sets up all required repositories for the home server

set -e  # Exit on error

# Configuration
REPOS_DIR="${REPOS_DIR:-$HOME/home-server-repos}"
MYSTERIUM_WALLET="${MYSTERIUM_WALLET:-}"

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Install Java 17 if not present
install_java() {
    log_info "Installing Java 17..."

    if command -v apt-get &> /dev/null; then
        sudo apt-get update
        sudo apt-get install -y openjdk-17-jdk
    elif command -v dnf &> /dev/null; then
        sudo dnf install -y java-17-openjdk java-17-openjdk-devel
    elif command -v yum &> /dev/null; then
        sudo yum install -y java-17-openjdk java-17-openjdk-devel
    elif command -v pacman &> /dev/null; then
        sudo pacman -S --noconfirm jdk17-openjdk
    elif command -v brew &> /dev/null; then
        brew install openjdk@17
    else
        log_error "Could not detect package manager. Please install Java 17 manually."
        return 1
    fi

    log_success "Java 17 installed."
}

# Install Docker if not present
install_docker() {
    log_info "Installing Docker..."

    if command -v apt-get &> /dev/null || command -v yum &> /dev/null || command -v dnf &> /dev/null; then
        curl -fsSL https://get.docker.com | sh
        sudo usermod -aG docker "$USER"
        log_success "Docker installed. You may need to log out and back in for group changes to take effect."
    elif command -v brew &> /dev/null; then
        brew install --cask docker
        log_success "Docker installed."
    else
        log_error "Could not install Docker automatically. Please install manually."
        return 1
    fi
}

# Check for required tools and install if missing
check_dependencies() {
    log_info "Checking dependencies..."

    # Git is required
    if ! command -v git &> /dev/null; then
        log_error "git is not installed. Please install git first."
        exit 1
    fi

    # Install Docker if not present
    if ! command -v docker &> /dev/null; then
        log_warn "Docker is not installed. Installing Docker..."
        install_docker || log_warn "Docker installation failed. Some services may not work."
    else
        log_success "Docker is installed."
    fi

    # Install Java if not present
    if ! command -v java &> /dev/null; then
        log_warn "Java is not installed. Installing Java 17..."
        install_java || log_warn "Java installation failed. Brainiac may not build."
    else
        # Check Java version
        java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
        if [ "$java_version" -lt 17 ] 2>/dev/null; then
            log_warn "Java version is less than 17. Installing Java 17..."
            install_java || log_warn "Java installation failed."
        else
            log_success "Java 17+ is installed."
        fi
    fi

    log_success "Dependency check complete."
}

# Create repos directory
setup_repos_directory() {
    log_info "Setting up repos directory at $REPOS_DIR..."
    mkdir -p "$REPOS_DIR"
    log_success "Repos directory ready."
}

# Clone a repository if it doesn't exist
clone_repo() {
    local repo_url="$1"
    local repo_name="$2"
    local repo_path="$REPOS_DIR/$repo_name"

    if [ -d "$repo_path" ]; then
        log_info "$repo_name already exists, pulling latest changes..."
        cd "$repo_path"
        git pull origin main || git pull origin master || log_warn "Could not pull latest changes for $repo_name"
        cd - > /dev/null
    else
        log_info "Cloning $repo_name..."
        git clone "$repo_url" "$repo_path"
    fi
    log_success "$repo_name cloned/updated."
}

# Setup Mysterium VPN
setup_mysterium() {
    log_info "=========================================="
    log_info "Setting up Mysterium VPN..."
    log_info "=========================================="

    local repo_path="$REPOS_DIR/mysterium-vpn-raspberry-pi"

    clone_repo "https://github.com/simon-duchastel/mysterium-vpn-raspberry-pi.git" "mysterium-vpn-raspberry-pi"

    cd "$repo_path"
    chmod +x setup.sh 2>/dev/null || true

    if [ -n "$MYSTERIUM_WALLET" ]; then
        log_info "Running Mysterium setup with provided wallet address..."
        echo "$MYSTERIUM_WALLET" | sudo ./setup.sh
    else
        log_warn "MYSTERIUM_WALLET not set. Running interactive setup..."
        log_warn "You will be prompted for your Mysterium wallet address."
        sudo ./setup.sh
    fi

    cd - > /dev/null
    log_success "Mysterium VPN setup complete."
}

# Setup HoneyGain
setup_honeygain() {
    log_info "=========================================="
    log_info "Setting up HoneyGain..."
    log_info "=========================================="

    local repo_path="$REPOS_DIR/honeygain-config-raspberry-pi"

    clone_repo "https://github.com/simon-duchastel/honeygain-config-raspberry-pi.git" "honeygain-config-raspberry-pi"

    cd "$repo_path"
    chmod +x setup.sh 2>/dev/null || true

    # Check if config exists
    if [ ! -f "honeygain.env.local" ]; then
        if [ -f "honeygain.env" ]; then
            log_warn "honeygain.env.local not found. Creating from template..."
            cp honeygain.env honeygain.env.local
            log_warn "Please edit $repo_path/honeygain.env.local with your HoneyGain credentials."
            log_warn "Then run: cd $repo_path && ./setup.sh"
        else
            log_warn "No honeygain.env template found. Please configure manually."
        fi
    else
        log_info "Running HoneyGain setup..."
        ./setup.sh
    fi

    cd - > /dev/null
    log_success "HoneyGain setup complete."
}

# Setup EarnApp
setup_earnapp() {
    log_info "=========================================="
    log_info "Setting up EarnApp..."
    log_info "=========================================="

    local repo_path="$REPOS_DIR/earnapp-config-raspberry-pi"

    clone_repo "https://github.com/simon-duchastel/earnapp-config-raspberry-pi.git" "earnapp-config-raspberry-pi"

    cd "$repo_path"
    chmod +x setup.sh 2>/dev/null || true

    log_info "Running EarnApp setup..."
    ./setup.sh

    cd - > /dev/null
    log_success "EarnApp setup complete."
    log_info "Remember to register your device at earnapp.com after setup."
}

# Setup Brainiac
setup_brainiac() {
    log_info "=========================================="
    log_info "Setting up Brainiac..."
    log_info "=========================================="

    local repo_path="$REPOS_DIR/brainiac"

    clone_repo "https://github.com/simon-duchastel/brainiac.git" "brainiac"

    cd "$repo_path"

    # Build with Gradle
    log_info "Building Brainiac..."
    if [ -f "gradlew" ]; then
        chmod +x gradlew
        ./gradlew build || log_warn "Brainiac build failed."
    else
        log_warn "gradlew not found. Please build manually with Gradle."
    fi

    cd - > /dev/null
    log_success "Brainiac setup complete."
}

# Main setup function
main() {
    echo ""
    echo "=============================================="
    echo "       Home Server Setup Script"
    echo "=============================================="
    echo ""

    check_dependencies
    setup_repos_directory

    # Setup each service
    setup_mysterium
    echo ""

    setup_honeygain
    echo ""

    setup_earnapp
    echo ""

    setup_brainiac
    echo ""

    echo "=============================================="
    log_success "All services have been set up!"
    echo "=============================================="
    echo ""
    log_info "Repos location: $REPOS_DIR"
    echo ""
    log_info "Next steps:"
    echo "  1. Configure HoneyGain credentials in $REPOS_DIR/honeygain-config-raspberry-pi/honeygain.env.local"
    echo "  2. Register your EarnApp device at earnapp.com"
    echo "  3. Check Mysterium node status with: cd $REPOS_DIR/mysterium-vpn-raspberry-pi && ./status.sh"
    echo ""
    log_info "To tear down all services, run: ./teardown.sh"
}

# Run main
main "$@"
