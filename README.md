# PetZania 🐾

<div align="center">
   <img src="https://github.com/user-attachments/assets/90548fe0-e7ae-478c-932d-57102f966c2e" width="200" alt="PetZania Logo">
  <h3>Connecting Pet Lovers Worldwide</h3>
</div>

## 📖 Overview

PetZania is a comprehensive social media platform designed to connect pet owners, facilitate pet adoption and breeding, and provide a trusted environment for pet-related services. The platform enables users to share their pet's experiences, find nearby pet services, engage with a passionate pet-loving community, and manage pet-related activities through an intuitive mobile application.

## 🏗️ Architecture

PetZania follows a **microservices architecture** with the following components:

- **Frontend**: React Native mobile application with Expo
- **Backend**: Multiple Spring Boot microservices
- **Database**: PostgreSQL for relational data
- **Message Broker**: RabbitMQ for inter-service communication
- **Caching**: Redis for performance optimization
- **Containerization**: Docker for deployment and orchestration

## 🛠️ Tech Stack

### Frontend Technologies
- **React Native** - Cross-platform mobile development
- **Expo** - Development platform and tools
- **React Navigation** - Navigation between screens
- **React Hook Form** - Form management and validation
- **Yup** - Schema validation
- **Axios** - HTTP client for API communication
- **Expo Router** - File-based routing system
- **React Query (TanStack Query)** – Server-state caching and synchronization

### Backend Technologies
- **Spring Boot** - Microservices framework
- **Spring Security** - Authentication and authorization
- **Spring Data JPA** - Database access layer
- **Spring WebSocket** - Real-time communication
- **Spring AMQP** - Message queuing with RabbitMQ
- **JWT** - Token-based authentication
- **Lombok** - Code generation and boilerplate reduction
- **Maven** - Dependency management and build tool

### Database & Infrastructure
- **PostgreSQL** - Primary relational database
- **Redis** - Caching and session management
- **RabbitMQ** - Message broker for inter-service communication
- **Docker** - Containerization and deployment
- **Docker Compose** - Multi-container orchestration

### AI & Machine Learning
- **DJL (Deep Java Library)** - Java-based deep learning framework
- **PyTorch Engine** - ML model inference
- **Hugging Face Tokenizers** - Natural language processing
- **Toxicity Detection** - Content moderation using pre-trained models

### Development Tools
- **SpringDoc OpenAPI** - API documentation

## 🚀 Features

### Core Functionality
- ✅ **User Registration & Authentication** - Secure JWT-based authentication system
- ✅ **Pet Profile Management** - Comprehensive pet profiles with photos and details
- ✅ **Social Media Features** - Posts, chats, likes, and user interactions
- ✅ **Real-time Messaging** - WebSocket-based chat system between users
- ✅ **Pet Adoption & Breeding** - Trusted platform for pet adoption and breeding services
- ✅ **Push Notifications** - Real-time notifications for user activities
- ✅ **Content Moderation** - AI-powered toxicity detection for user-generated content
- ✅ Smart caching & auto-refetch with React Query (TanStack)


### Advanced Features
- ✅ **Microservices Architecture** - Scalable and maintainable backend design
- ✅ **Message Queuing** - Asynchronous processing with RabbitMQ
- ✅ **Caching Layer** - Redis-based performance optimization
- ✅ **API Documentation** - Comprehensive OpenAPI/Swagger documentation
- ✅ **Cross-platform Mobile App** - iOS and Android support via React Native
- ✅ **Responsive Design** - Adaptive UI for different screen sizes

## 📱 Mobile App Features

- **Onboarding Experience** - Guided setup for new users
- **Tab-based Navigation** - Intuitive app structure
- **Image Upload** - Pet photo management
- **Form Validation** - Real-time input validation
- **Offline Support** - Basic offline functionality
- **Push Notifications** - Real-time updates and alerts

## 🏢 Microservices

### 1. Registration Module (`:8080`)
- User registration and authentication
- Profile management
- JWT token generation and validation

### 2. Friends & Chats Module (`:8081`)
- User connections and friend management
- Real-time messaging system
- Chat history and conversation management

### 3. Adoption & Breeding Module (`:8082`)
- Pet adoption listings and management
- Breeding services and coordination
- Content moderation with AI toxicity detection

### 4. Notification Module (`:8083`)
- Push notification management
- WebSocket-based real-time notifications
- Notification preferences and settings

## 🚀 Getting Started

### Prerequisites
- Node.js (v18+)
- Java 21
- Docker & Docker Compose
- PostgreSQL
- Redis
- RabbitMQ

### Frontend Setup
```bash
cd Front-End/petzania
npm install
npx expo start
```

### Backend Setup
```bash
cd Back-End
docker-compose up -d
```

### Individual Service Setup
```bash
# Registration Module
cd Back-End/registration-module
mvn spring-boot:run

# Friends & Chats Module
cd Back-End/friends-and-chats-module
mvn spring-boot:run

# Adoption & Breeding Module
cd Back-End/adoption-and-breeding-module
mvn spring-boot:run

# Notification Module
cd Back-End/notification-module
mvn spring-boot:run
```

## 📚 API Documentation

Each microservice provides comprehensive API documentation via Swagger UI:

- **Registration Module**: http://157.230.114.107:8080/swagger-ui.html
- **Friends & Chats Module**: http://157.230.114.107:8081/swagger-ui.html
- **Adoption & Breeding Module**: http://157.230.114.107:8082/swagger-ui.html
- **Notification Module**: http://157.230.114.107:8083/swagger-ui.html

## 🐳 Docker Deployment

### Using Docker Compose
```bash
cd Back-End
docker-compose up -d
```

### Individual Service Deployment
```bash
# Build and run individual services
docker build -t petzania-registration ./registration-module
docker build -t petzania-friends ./friends-and-chats-module
docker build -t petzania-adoption ./adoption-and-breeding-module
docker build -t petzania-notification ./notification-module
```

## 🔧 Configuration

### Environment Variables
Key configuration files are located in:
- `Back-End/*/src/main/resources/application.yml`
- `Front-End/petzania/app.json`

### Database Configuration
- PostgreSQL connection details in `docker-compose.yml`
- Database initialization scripts in `Back-End/init-db.sql`

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 🔮 Roadmap

- [ ] Enhanced AI features for pet matching
- [ ] Video calling integration
- [ ] Pet health tracking
- [ ] Veterinary appointment scheduling
- [ ] Pet insurance integration
- [ ] Advanced analytics dashboard

## 👥 Collaborators
| Name | LinkedIn |
|------|---------|
| Ali Tarek Ahmed Ibrahim | [LinkedIn](https://www.linkedin.com/in/ali-tarek517/) |
| Beshoy Hany Attia | [LinkedIn](https://www.linkedin.com/in/beshoyhanyy/) |
| Alan Samir Hakoun | [LinkedIn](https://www.linkedin.com/in/alan-hakoun/) |
| Mohamed Khaled El-Sayed | [LinkedIn](https://www.linkedin.com/in/mohamedkhaledomran/) |
| Ahmed Mohamed Abd El-Wahab | [LinkedIn](https://www.linkedin.com/in/ahmed-abd-el-wahab-9b91a623b/) |
---

<div align="center">
  <p>Made with ❤️ for pet lovers worldwide</p>
  <p>🐕 🐈 🐠 🐹</p>
</div> 
