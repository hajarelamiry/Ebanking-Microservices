const { ApolloServer } = require('@apollo/server');
const { startStandaloneServer } = require('@apollo/server/standalone');
const axios = require('axios');
require('dotenv').config();

const AUTH_SERVICE_URL = process.env.AUTH_SERVICE_URL || 'http://localhost:8081';
const USER_SERVICE_URL = process.env.USER_SERVICE_URL || 'http://localhost:8082';

const typeDefs = `#graphql
  type Transaction {
    id: ID
    amount: Float
    currency: String
    description: String
    date: String
    type: String
    category: String
    icon: String
  }

  type UserProfile {
    id: ID
    keycloakId: String
    firstName: String
    lastName: String
    email: String
    phoneNumber: String
    address: String
    kycStatus: String
    kycDocumentUrl: String
  }

  type AuthStatus {
    isAuthenticated: Boolean
    username: String
    roles: [String]
  }

  type UserData {
    profile: UserProfile
    auth: AuthStatus
  }

  type Query {
    me: UserData
    getUser(id: ID!): UserProfile
    users: [UserProfile]
    transactions: [Transaction]
    publicInfo: String
    pendingRequests: [UserCreationRequest]
  }

  type UserCreationRequest {
    id: ID
    firstName: String
    lastName: String
    email: String
    status: RequestStatus
    agentId: String
    createdAt: String
  }

  enum RequestStatus {
    PENDING
    APPROVED
    REJECTED
  }

  input UserRequestInput {
    firstName: String!
    lastName: String!
    email: String!
  }

  input ProfileInput {
    firstName: String
    lastName: String
    phoneNumber: String
    address: String
  }

  type Mutation {
    updateProfile(input: ProfileInput!): UserProfile
    sendMoney(recipient: String!, amount: Float!, description: String): Transaction
    submitKyc(documentUrl: String!): UserProfile
    validateKyc(id: ID!): UserProfile
    createCustomer(input: CreateCustomerInput!): UserProfile
    deleteUser(id: ID!): Boolean
    requestUserCreation(input: UserRequestInput!): UserCreationRequest
    processUserCreation(id: ID!, status: RequestStatus!, reason: String): UserCreationRequest
    onboardUser(input: UserRequestInput!): UserProfile
  }

  input CreateCustomerInput {
    keycloakId: String!
    firstName: String!
    lastName: String!
    email: String!
  }
`;

const callGraphQL = async (url, data, token) => {
    console.log(`Calling GraphQL at ${url} with body: ${JSON.stringify(data).substring(0, 100)}...`);
    try {
        const response = await axios.post(url, data, {
            headers: { Authorization: token }
        });
        console.log(`Success response from ${url}: ${response.status}`);
        console.log(`Response data from ${url}:`, JSON.stringify(response.data));
        if (response.data.errors) {
            console.error(`GraphQL errors from ${url}:`, JSON.stringify(response.data.errors));
            throw new Error(response.data.errors[0].message);
        }
        return response.data;
    } catch (error) {
        console.error(`Axios error calling ${url}:`, error.response ? `${error.response.status} - ${JSON.stringify(error.response.data)}` : error.message);
        throw error;
    }
};

const resolvers = {
    Query: {
        publicInfo: async () => {
            try {
                const response = await axios.get(`${AUTH_SERVICE_URL}/auth/public`);
                return response.data;
            } catch (error) {
                console.error('Error fetching public info:', error.message);
                return 'Offline';
            }
        },
        me: async (_, __, { token }) => {
            console.log('--- me query started ---');
            try {
                // 1. Check auth via auth-service
                console.log('Validating token with auth-service...');
                try {
                    await axios.get(`${AUTH_SERVICE_URL}/auth/protected`, {
                        headers: { Authorization: token }
                    });
                    console.log('Token validated successfully.');
                } catch (authError) {
                    console.error('Auth-service validation failed:', authError.response ? authError.response.status : authError.message);
                    throw new Error('Authentication failed');
                }

                // 2. Get profile info from user-service via GraphQL
                console.log('Fetching profile from user-service...');
                const profileQuery = {
                    query: `
                        query {
                            me {
                                id
                                keycloakId
                                firstName
                                lastName
                                email
                                phoneNumber
                                address
                                kycStatus
                            }
                        }
                    `
                };

                const responseData = await callGraphQL(`${USER_SERVICE_URL}/graphql`, profileQuery, token);
                const profileData = responseData.data.me;
                console.log('Profile fetched:', profileData ? profileData.email : 'null');

                return {
                    profile: profileData,
                    auth: {
                        isAuthenticated: true,
                        username: profileData ? profileData.email : 'Unknown',
                        roles: []
                    }
                };
            } catch (error) {
                console.error('Final error in me query:', error.message);
                throw new Error(error.message || 'Unauthorized or Service Unavailable');
            }
        },
        users: async (_, __, { token }) => {
            console.log('--- Users Query Started ---');
            try {
                const query = {
                    query: `
                        query {
                            users {
                                keycloakId
                                firstName
                                lastName
                                email
                                kycStatus
                                kycDocumentUrl
                            }
                        }
                    `
                };
                const responseData = await callGraphQL(`${USER_SERVICE_URL}/graphql`, query, token);
                console.log('--- Users Query Success ---');
                return responseData.data.users;
            } catch (error) {
                console.error('--- Users Query Failed ---');
                console.error('Error Details:', error.message);
                throw new Error('Failed to fetch users');
            }
        },
        transactions: async () => {
            return [
                { id: '1', amount: 45.0, currency: 'USD', description: 'Grocery Store', date: '2026-01-01', type: 'EXPENSE', category: 'Shopping', icon: 'bi-cart' },
                { id: '2', amount: 120.0, currency: 'USD', description: 'Monthly Utilities', date: '2026-01-01', type: 'EXPENSE', category: 'Utilities', icon: 'bi-lightning' },
                { id: '3', amount: 450.0, currency: 'USD', description: 'Travel Tickets', date: '2025-12-30', type: 'EXPENSE', category: 'Travel', icon: 'bi-airplane' },
                { id: '4', amount: 2500.0, currency: 'USD', description: 'Salary Deposit', date: '2025-12-28', type: 'INCOME', category: 'Income', icon: 'bi-cash-stack' },
            ];
        },
        pendingRequests: async (_, __, { token }) => {
            console.log('--- PendingRequests Query Started ---');
            try {
                const query = {
                    query: `
                        query {
                            pendingRequests {
                                id
                                firstName
                                lastName
                                email
                                status
                                agentId
                                createdAt
                            }
                        }
                    `
                };
                const responseData = await callGraphQL(`${USER_SERVICE_URL}/graphql`, query, token);
                console.log('--- PendingRequests Query Success ---');
                return responseData.data.pendingRequests;
            } catch (error) {
                console.error('--- PendingRequests Query Failed ---');
                console.error('Error Details:', error.message);
                throw new Error('Failed to fetch pending requests');
            }
        }
    },
    Mutation: {
        updateProfile: async (_, { input }, { token }) => {
            try {
                const mutation = {
                    query: `
                        mutation UpdateProfile($input: ProfileInput!) {
                            updateProfile(input: $input) {
                                firstName
                                lastName
                                phoneNumber
                                address
                            }
                        }
                    `,
                    variables: { input }
                };
                const responseData = await callGraphQL(`${USER_SERVICE_URL}/graphql`, mutation, token);
                return responseData.data.updateProfile;
            } catch (error) {
                console.error('Error in updateProfile mutation:', error.message);
                throw new Error('Failed to update profile');
            }
        },
        sendMoney: async (_, { recipient, amount, description }) => {
            console.log(`Simulating transfer of ${amount} to ${recipient}`);
            return {
                id: Math.random().toString(36).substr(2, 9),
                amount: amount,
                currency: 'USD',
                description: `Transfer to ${recipient}: ${description || ''}`,
                date: new Date().toISOString().split('T')[0],
                type: 'EXPENSE',
                category: 'Transfer',
                icon: 'bi-send'
            };
        },
        submitKyc: async (_, { documentUrl }, { token }) => {
            try {
                const mutation = {
                    query: `
                        mutation SubmitKyc($documentUrl: String!) {
                            submitKyc(documentUrl: $documentUrl) {
                                kycStatus
                            }
                        }
                    `,
                    variables: { documentUrl }
                };
                const responseData = await callGraphQL(`${USER_SERVICE_URL}/graphql`, mutation, token);
                return responseData.data.submitKyc;
            } catch (error) {
                console.error('Error in submitKyc mutation:', error.message);
                throw new Error(error.message || 'Failed to submit KYC via GraphQL');
            }
        },
        validateKyc: async (_, { id }, { token }) => {
            try {
                const mutation = {
                    query: `
                        mutation ValidateKyc($id: ID!) {
                            validateKyc(id: $id) {
                                kycStatus
                            }
                        }
                    `,
                    variables: { id }
                };
                const responseData = await callGraphQL(`${USER_SERVICE_URL}/graphql`, mutation, token);
                return responseData.data.validateKyc;
            } catch (error) {
                console.error('Error in validateKyc mutation:', error.message);
                throw new Error('Failed to validate KYC');
            }
        },
        createCustomer: async (_, { input }, { token }) => {
            try {
                const mutation = {
                    query: `
                        mutation CreateCustomer($input: CreateCustomerInput!) {
                            createCustomer(input: $input) {
                                keycloakId
                                firstName
                                lastName
                                email
                                kycStatus
                            }
                        }
                    `,
                    variables: { input }
                };
                const responseData = await callGraphQL(`${USER_SERVICE_URL}/graphql`, mutation, token);
                return responseData.data.createCustomer;
            } catch (error) {
                console.error('Error in createCustomer mutation:', error.message);
                throw new Error(error.message || 'Failed to create customer');
            }
        },
        deleteUser: async (_, { id }, { token }) => {
            try {
                const mutation = {
                    query: `
                        mutation DeleteUser($id: ID!) {
                            deleteUser(id: $id)
                        }
                    `,
                    variables: { id }
                };
                const responseData = await callGraphQL(`${USER_SERVICE_URL}/graphql`, mutation, token);
                return responseData.data.deleteUser;
            } catch (error) {
                console.error('Error in deleteUser mutation:', error.message);
                throw new Error(error.message || 'Failed to delete user');
            }
        },
        requestUserCreation: async (_, { input }, { token }) => {
            console.log('--- RequestUserCreation Mutation Started ---');
            console.log('Input:', JSON.stringify(input));
            try {
                const mutation = {
                    query: `
                        mutation RequestUserCreation($input: UserRequestInput!) {
                            requestUserCreation(input: $input) {
                                id
                                status
                            }
                        }
                    `,
                    variables: { input }
                };
                const responseData = await callGraphQL(`${USER_SERVICE_URL}/graphql`, mutation, token);
                console.log('--- RequestUserCreation Mutation Success ---');
                return responseData.data.requestUserCreation;
            } catch (error) {
                console.error('--- RequestUserCreation Mutation Failed ---');
                console.error('Error Details:', error.message);
                if (error.response) {
                    console.error('Response Status:', error.response.status);
                    console.error('Response Data:', JSON.stringify(error.response.data));
                }
                throw new Error(error.message || 'Failed to submit user creation request');
            }
        },
        processUserCreation: async (_, { id, status, reason }, { token }) => {
            try {
                const mutation = {
                    query: `
                        mutation ProcessUserCreation($id: ID!, $status: RequestStatus!, $reason: String) {
                            processUserCreation(id: $id, status: $status, reason: $reason) {
                                id
                                status
                            }
                        }
                    `,
                    variables: { id, status, reason }
                };
                const responseData = await callGraphQL(`${USER_SERVICE_URL}/graphql`, mutation, token);
                return responseData.data.processUserCreation;
            } catch (error) {
                console.error('Error in processUserCreation mutation:', error.message);
                throw new Error(error.message || 'Failed to process user creation request');
            }
        },
        onboardUser: async (_, { input }, { token }) => {
            console.log('--- OnboardUser Mutation Started ---');
            console.log('Input:', JSON.stringify(input));
            try {
                const mutation = {
                    query: `
                        mutation OnboardUser($input: UserRequestInput!) {
                            onboardUser(input: $input) {
                                keycloakId
                                firstName
                                lastName
                                email
                            }
                        }
                    `,
                    variables: { input }
                };
                const responseData = await callGraphQL(`${USER_SERVICE_URL}/graphql`, mutation, token);
                console.log('--- OnboardUser Mutation Success ---');
                return responseData.data.onboardUser;
            } catch (error) {
                console.error('--- OnboardUser Mutation Failed ---');
                console.error('Error Details:', error.message);
                if (error.response) {
                    console.error('Response Status:', error.response.status);
                    console.error('Response Data:', JSON.stringify(error.response.data));
                }
                throw new Error(error.message || 'Failed to onboard user');
            }
        }
    }
};

const server = new ApolloServer({
    typeDefs,
    resolvers,
});

startStandaloneServer(server, {
    listen: { port: 4000 },
    context: async ({ req }) => {
        const token = req.headers.authorization || '';
        return { token };
    },
}).then(({ url }) => {
    console.log(`🚀 Gateway ready at ${url}`);
});
