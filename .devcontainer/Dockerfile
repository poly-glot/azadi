FROM mcr.microsoft.com/devcontainers/base:ubuntu

# Node.js 22
RUN curl -fsSL https://deb.nodesource.com/setup_22.x | bash - \
    && apt-get install -y nodejs \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# Firebase CLI
RUN npm install -g firebase-tools

# Create cache directories
RUN mkdir -p /home/vscode/.m2 /home/vscode/.npm \
    && chown -R vscode:vscode /home/vscode/.m2 /home/vscode/.npm

USER vscode
WORKDIR /workspace
