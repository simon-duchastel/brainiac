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

# Check for required tools
check_dependencies() {
    log_info "Checking dependencies..."

    if ! command -v git &> /dev/null; then
        log_error "git is not installed. Please install git first."
        exit 1
    fi

    if ! command -v docker &> /dev/null; then
        log_warn "Docker is not installed. Some services may require Docker."
        log_warn "Install Docker with: curl -fsSL https://get.docker.com | sh"
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

    # Check for Java/Gradle
    if command -v java &> /dev/null; then
        log_info "Building Brainiac..."
        if [ -f "gradlew" ]; then
            chmod +x gradlew
            ./gradlew build || log_warn "Brainiac build failed. You may need to install Java 17+."
        else
            log_warn "gradlew not found. Please build manually with Gradle."
        fi
    else
        log_warn "Java not installed. Brainiac requires Java 17+ to build."
        log_warn "Install Java and then run: cd $repo_path && ./gradlew build"
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
