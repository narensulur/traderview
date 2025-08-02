#!/bin/bash

# Deploy TraderView from GitHub to AWS Amplify
# Account: 976071095692
# Repository: https://github.com/narensulur/traderview

echo "🚀 Deploying TraderView from GitHub to AWS Amplify..."

# Configuration
APP_NAME="traderview-frontend"
REGION="us-east-1"
ACCOUNT_ID="976071095692"
GITHUB_REPO="https://github.com/narensulur/traderview"
BRANCH_NAME="main"

# Verify AWS account
CURRENT_ACCOUNT=$(aws sts get-caller-identity --query Account --output text 2>/dev/null)
if [ "$CURRENT_ACCOUNT" != "$ACCOUNT_ID" ]; then
    echo "❌ Please configure AWS CLI with account $ACCOUNT_ID"
    echo "Run: aws configure"
    exit 1
fi

echo "✅ AWS Account verified: $ACCOUNT_ID"

# Create Amplify app
echo "📱 Creating Amplify app..."
APP_ID=$(aws amplify create-app \
    --name "$APP_NAME" \
    --description "TraderView Frontend - React Trading Journal Application" \
    --repository "$GITHUB_REPO" \
    --platform WEB \
    --build-spec '{
        "version": 1,
        "frontend": {
            "phases": {
                "preBuild": {
                    "commands": [
                        "cd plugin-frontend",
                        "npm ci"
                    ]
                },
                "build": {
                    "commands": [
                        "npm run build"
                    ]
                }
            },
            "artifacts": {
                "baseDirectory": "plugin-frontend/build",
                "files": [
                    "**/*"
                ]
            },
            "cache": {
                "paths": [
                    "plugin-frontend/node_modules/**/*"
                ]
            }
        }
    }' \
    --environment-variables \
        REACT_APP_API_BASE_URL=https://your-backend-api-url.com,\
        REACT_APP_GOOGLE_CLIENT_ID=893822634946-iqnq8b93tsbd7733f7rnuc7l13mhlqkb.apps.googleusercontent.com,\
        REACT_APP_ROBINHOOD_CLIENT_ID=demo-robinhood-client-id,\
        REACT_APP_TRADESTATION_CLIENT_ID=demo-tradestation-client-id \
    --query 'app.appId' \
    --output text)

if [ $? -ne 0 ]; then
    echo "❌ Failed to create Amplify app"
    exit 1
fi

echo "✅ Amplify app created with ID: $APP_ID"

# Create branch
echo "🌿 Creating branch: $BRANCH_NAME"
aws amplify create-branch \
    --app-id "$APP_ID" \
    --branch-name "$BRANCH_NAME" \
    --description "Main production branch" \
    --enable-auto-build

if [ $? -ne 0 ]; then
    echo "❌ Failed to create branch"
    exit 1
fi

echo "✅ Branch created successfully"

# Start deployment
echo "🚀 Starting deployment..."
JOB_ID=$(aws amplify start-job \
    --app-id "$APP_ID" \
    --branch-name "$BRANCH_NAME" \
    --job-type RELEASE \
    --query 'jobSummary.jobId' \
    --output text)

echo "✅ Deployment started with Job ID: $JOB_ID"

# Get app URL
APP_URL="https://$BRANCH_NAME.$APP_ID.amplifyapp.com"

echo ""
echo "🎉 Deployment initiated successfully!"
echo ""
echo "📊 Amplify Console: https://console.aws.amazon.com/amplify/home?region=$REGION#/$APP_ID"
echo "🌐 App URL (available after build): $APP_URL"
echo "📝 Job Status: https://console.aws.amazon.com/amplify/home?region=$REGION#/$APP_ID/$BRANCH_NAME/$JOB_ID"
echo ""
echo "⏳ Build typically takes 2-5 minutes..."
echo ""
echo "📋 Next Steps:"
echo "   1. Monitor build progress in Amplify Console"
echo "   2. Update Google OAuth redirect URIs with: $APP_URL"
echo "   3. Configure your backend API URL in environment variables"
echo "   4. Set up custom domain (optional)"
echo ""
echo "🔧 To update environment variables:"
echo "   aws amplify update-app --app-id $APP_ID --environment-variables REACT_APP_API_BASE_URL=https://your-api-url.com"
