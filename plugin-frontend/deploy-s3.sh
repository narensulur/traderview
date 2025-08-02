#!/bin/bash

# Deploy TraderView to S3 + CloudFront
# Account: 976071095692

echo "🚀 Deploying TraderView to S3..."

# Configuration
BUCKET_NAME="traderview-app-$(date +%s)"
REGION="us-east-1"
ACCOUNT_ID="976071095692"

# Verify AWS account
CURRENT_ACCOUNT=$(aws sts get-caller-identity --query Account --output text 2>/dev/null)
if [ "$CURRENT_ACCOUNT" != "$ACCOUNT_ID" ]; then
    echo "❌ Please configure AWS CLI with account $ACCOUNT_ID"
    echo "Run: aws configure"
    exit 1
fi

echo "✅ AWS Account verified: $ACCOUNT_ID"

# Build the app
echo "📦 Building React application..."
npm run build

if [ $? -ne 0 ]; then
    echo "❌ Build failed!"
    exit 1
fi

echo "✅ Build completed successfully"

# Create S3 bucket
echo "🪣 Creating S3 bucket: $BUCKET_NAME"
aws s3 mb s3://$BUCKET_NAME --region $REGION

if [ $? -ne 0 ]; then
    echo "❌ Failed to create S3 bucket"
    exit 1
fi

# Configure static website hosting
echo "🌐 Configuring static website hosting..."
aws s3 website s3://$BUCKET_NAME --index-document index.html --error-document index.html

# Upload files
echo "📤 Uploading files to S3..."
aws s3 sync build/ s3://$BUCKET_NAME --delete

# Set bucket policy for public access
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

# Get website URL
WEBSITE_URL="http://$BUCKET_NAME.s3-website-$REGION.amazonaws.com"

echo ""
echo "🎉 Deployment completed successfully!"
echo ""
echo "🌐 Website URL: $WEBSITE_URL"
echo "🪣 S3 Bucket: $BUCKET_NAME"
echo "📊 S3 Console: https://s3.console.aws.amazon.com/s3/buckets/$BUCKET_NAME"
echo ""
echo "📋 Next Steps:"
echo "   1. Test your app: $WEBSITE_URL"
echo "   2. Update Google OAuth redirect URIs with: $WEBSITE_URL"
echo "   3. Set up CloudFront for HTTPS (optional)"
echo "   4. Configure custom domain (optional)"
