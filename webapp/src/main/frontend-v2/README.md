# Frontend V2

This project attempts to modernize the Mojito front-end. The goal is to deploy both and slowly port functionality over between the two.

## Getting Started

### Prerequisites

Running mvn clean install at the root installs the correct node & pnpm versions

Before running the application locally, ensure you have the correct Node.js and pnpm versions:

```bash
source use_local_npm.sh
```

This script will configure your environment to use the appropriate versions of Node.js and npm required for this project.

## Configuration

The application behaves differently depending on the environment mode:

### Development Mode

In development mode, the application:
- Uses routes with the base path `/`
- Has a proxy configured to forward API requests to the local backend server
- Enables hot reloading and development tools

### Production Mode

In production mode, the application:
- Uses routes with the base path `/v2/`
- Makes direct API calls without proxy forwarding

## Running the Application

### Development

```bash
# Make sure you're using the correct npm version
source use_local_npm.sh

# Install dependencies
npm install

# Start the development server
npm run dev
```

### Production Build

```bash
# Make sure you're using the correct npm version
source use_local_npm.sh

# Install dependencies
npm install

# Create production build
npm run build
```
