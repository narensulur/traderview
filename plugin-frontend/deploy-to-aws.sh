#!/bin/bash

# TraderView Frontend Deployment Script for AWS
# Account: 976071095692

echo "🚀 Deploying TraderView Frontend to AWS..."

# Configuration
APP_NAME="traderview-frontend"
REGION="us-east-1"  # Change if you prefer a different region
ACCOUNT_ID="976071095692"

# Check if AWS CLI is configured
if ! aws sts get-caller-identity > /dev/null 2>&1; then
    echo "❌ AWS CLI not configured. Please run 'aws configure' first."
    exit 1
fi

# Verify account
CURRENT_ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
if [ "$CURRENT_ACCOUNT" != "$ACCOUNT_ID" ]; then
    echo "❌ Wrong AWS account. Expected: $ACCOUNT_ID, Current: $CURRENT_ACCOUNT"
    exit 1
fi

echo "✅ AWS Account verified: $ACCOUNT_ID"

# Build the React app
echo "📦 Building React application..."
npm run build

if [ $? -ne 0 ]; then
    echo "❌ Build failed!"
    exit 1
fi

echo "✅ Build completed successfully"

# Option 1: Deploy to S3 + CloudFront
deploy_s3() {
    BUCKET_NAME="traderview-app-$(date +%s)"
    
    echo "🪣 Creating S3 bucket: $BUCKET_NAME"
    aws s3 mb s3://$BUCKET_NAME --region $REGION
    
    echo "🌐 Configuring static website hosting..."
    aws s3 website s3://$BUCKET_NAME --index-document index.html --error-document index.html
    
    echo "📤 Uploading files to S3..."
    aws s3 sync build/ s3://$BUCKET_NAME --delete
    
    echo "🔓 Setting bucket policy for public access..."
    cat > bucket-policy.json << EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "PublicReadGetObject",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::$BUCKET_NAME/*"
    }
  ]
}
EOF
    
    aws s3api put-bucket-policy --bucket $BUCKET_NAME --policy file://bucket-policy.json
    rm bucket-policy.json
    
    WEBSITE_URL="http://$BUCKET_NAME.s3-website-$REGION.amazonaws.com"
    echo "✅ Deployment complete!"
    echo "🌐 Website URL: $WEBSITE_URL"
}

# Option 2: Deploy to Amplify
deploy_amplify() {
    echo "🔧 Creating Amplify app..."
    
    # Create Amplify app
    APP_ID=$(aws amplify create-app \
        --name $APP_NAME \
        --description "TraderView Frontend Application" \
        --repository "https://github.com/yourusername/traderview" \
        --platform WEB \
        --query 'app.appId' \
        --output text)
    
    echo "✅ Amplify app created: $APP_ID"
    
    # Create branch
    aws amplify create-branch \
        --app-id $APP_ID \
        --branch-name main \
        --description "Main production branch"
    
    echo "✅ Branch created"
    echo "🌐 Amplify Console: https://console.aws.amazon.com/amplify/home?region=$REGION#/$APP_ID"
}

# Ask user which deployment method to use
echo ""
echo "Choose deployment method:"
echo "1) S3 + CloudFront (Most cost-effective)"
echo "2) AWS Amplify (Easiest with CI/CD)"
echo ""
read -p "Enter choice (1 or 2): " choice

case $choice in
    1)
        deploy_s3
        ;;
    2)
        deploy_amplify
        ;;
    *)
        echo "❌ Invalid choice"
        exit 1
        ;;
esac

echo ""
echo "🎉 Deployment initiated successfully!"
echo "📝 Next steps:"
echo "   1. Update OAuth redirect URIs with your new domain"
echo "   2. Configure your backend API URL"
echo "   3. Set up custom domain (optional)"
