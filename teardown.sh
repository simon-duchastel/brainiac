#!/bin/bash

# Home Server Teardown Script
# This script stops services and cleans up all repositories

set -e  # Exit on error

# Configuration
REPOS_DIR="${REPOS_DIR:-$HOME/home-server-repos}"
REMOVE_REPOS="${REMOVE_REPOS:-false}"

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

# Teardown Mysterium VPN
teardown_mysterium() {
    log_info "=========================================="
    log_info "Tearing down Mysterium VPN..."
    log_info "=========================================="

    local repo_path="$REPOS_DIR/mysterium-vpn-raspberry-pi"

    if [ -d "$repo_path" ]; then
        cd "$repo_path"

        # Stop Mysterium service if running
        if systemctl is-active --quiet myst; then
            log_info "Stopping Mysterium service..."
            sudo systemctl stop myst || log_warn "Could not stop myst service"
            sudo systemctl disable myst || log_warn "Could not disable myst service"
        fi

        # Run teardown script if it exists
        if [ -f "teardown.sh" ]; then
            chmod +x teardown.sh
            sudo ./teardown.sh || log_warn "Mysterium teardown script failed"
        fi

        cd - > /dev/null
        log_success "Mysterium VPN stopped."
    else
        log_warn "Mysterium repo not found at $repo_path"
    fi
}

# Teardown HoneyGain
teardown_honeygain() {
    log_info "=========================================="
    log_info "Tearing down HoneyGain..."
    log_info "=========================================="

    local repo_path="$REPOS_DIR/honeygain-config-raspberry-pi"

    if [ -d "$repo_path" ]; then
        cd "$repo_path"

        # Stop HoneyGain Docker container
        if command -v docker &> /dev/null; then
            log_info "Stopping HoneyGain Docker container..."
            docker stop honeygain 2>/dev/null || log_warn "No honeygain container running"
            docker rm honeygain 2>/dev/null || log_warn "No honeygain container to remove"
        fi

        # Run teardown script if it exists
        if [ -f "teardown.sh" ]; then
            chmod +x teardown.sh
            ./teardown.sh || log_warn "HoneyGain teardown script failed"
        fi

        cd - > /dev/null
        log_success "HoneyGain stopped."
    else
        log_warn "HoneyGain repo not found at $repo_path"
    fi
}

# Teardown EarnApp
teardown_earnapp() {
    log_info "=========================================="
    log_info "Tearing down EarnApp..."
    log_info "=========================================="

    local repo_path="$REPOS_DIR/earnapp-config-raspberry-pi"

    if [ -d "$repo_path" ]; then
        cd "$repo_path"

        # Stop EarnApp service/container
        if command -v docker &> /dev/null; then
            log_info "Stopping EarnApp Docker container..."
            docker stop earnapp 2>/dev/null || log_warn "No earnapp container running"
            docker rm earnapp 2>/dev/null || log_warn "No earnapp container to remove"
        fi

        # Try to stop earnapp service if it exists
        if systemctl is-active --quiet earnapp 2>/dev/null; then
            log_info "Stopping EarnApp service..."
            sudo systemctl stop earnapp || log_warn "Could not stop earnapp service"
            sudo systemctl disable earnapp || log_warn "Could not disable earnapp service"
        fi

        # Run teardown script if it exists
        if [ -f "teardown.sh" ]; then
            chmod +x teardown.sh
            ./teardown.sh || log_warn "EarnApp teardown script failed"
        fi

        cd - > /dev/null
        log_success "EarnApp stopped."
    else
        log_warn "EarnApp repo not found at $repo_path"
    fi
}

# Teardown Brainiac
teardown_brainiac() {
    log_info "=========================================="
    log_info "Tearing down Brainiac..."
    log_info "=========================================="

    local repo_path="$REPOS_DIR/brainiac"

    if [ -d "$repo_path" ]; then
        cd "$repo_path"

        # Clean Gradle build
        if [ -f "gradlew" ]; then
            log_info "Cleaning Brainiac build..."
            ./gradlew clean 2>/dev/null || log_warn "Could not clean Brainiac build"
        fi

        # Run teardown script if it exists
        if [ -f "teardown.sh" ]; then
            chmod +x teardown.sh
            ./teardown.sh || log_warn "Brainiac teardown script failed"
        fi

        cd - > /dev/null
        log_success "Brainiac stopped."
    else
        log_warn "Brainiac repo not found at $repo_path"
    fi
}

# Remove all cloned repositories
remove_repos() {
    log_info "=========================================="
    log_info "Removing cloned repositories..."
    log_info "=========================================="

    if [ -d "$REPOS_DIR" ]; then
        log_warn "This will delete all repos in $REPOS_DIR"
        read -p "Are you sure? (y/N): " confirm

        if [[ "$confirm" =~ ^[Yy]$ ]]; then
            rm -rf "$REPOS_DIR"
            log_success "All repositories removed."
        else
            log_info "Repositories preserved."
        fi
    else
        log_warn "Repos directory not found at $REPOS_DIR"
    fi
}

# Show usage
usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  --remove-repos    Also delete all cloned repositories after stopping services"
    echo "  --help            Show this help message"
    echo ""
    echo "Environment Variables:"
    echo "  REPOS_DIR         Directory containing repos (default: ~/home-server-repos)"
    echo "  REMOVE_REPOS      Set to 'true' to remove repos (default: false)"
    echo ""
    echo "Examples:"
    echo "  $0                    # Stop all services, keep repos"
    echo "  $0 --remove-repos     # Stop all services and delete repos"
    echo "  REPOS_DIR=/opt/repos $0  # Use custom repos directory"
}

# Main teardown function
main() {
    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --remove-repos)
                REMOVE_REPOS="true"
                shift
                ;;
            --help|-h)
                usage
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                usage
                exit 1
                ;;
        esac
    done

    echo ""
    echo "=============================================="
    echo "       Home Server Teardown Script"
    echo "=============================================="
    echo ""

    log_info "Repos location: $REPOS_DIR"
    echo ""

    # Teardown each service (in reverse order of setup)
    teardown_brainiac
    echo ""

    teardown_earnapp
    echo ""

    teardown_honeygain
    echo ""

    teardown_mysterium
    echo ""

    # Optionally remove repos
    if [ "$REMOVE_REPOS" = "true" ]; then
        remove_repos
        echo ""
    fi

    echo "=============================================="
    log_success "All services have been torn down!"
    echo "=============================================="
    echo ""

    if [ "$REMOVE_REPOS" != "true" ]; then
        log_info "Repositories are still available at: $REPOS_DIR"
        log_info "To also remove repositories, run: $0 --remove-repos"
    fi
}

# Run main
main "$@"
