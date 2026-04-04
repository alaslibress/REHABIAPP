import { ApolloClient, InMemoryCache, createHttpLink, from } from '@apollo/client';
import { setContext } from '@apollo/client/link/context';
import { onError } from '@apollo/client/link/error';
import * as SecureStore from 'expo-secure-store';
import type { AuthToken } from '../../types/auth';

const TOKEN_KEY = 'auth_token';

// URL base del backend BFF — cambiar segun entorno
const GRAPHQL_URI = 'http://localhost:4000/graphql';

const httpLink = createHttpLink({
  uri: GRAPHQL_URI,
});

// Link de autenticacion: adjunta el JWT a cada peticion
const authLink = setContext(async function (_request, previousContext) {
  const tokenJson = await SecureStore.getItemAsync(TOKEN_KEY);
  if (!tokenJson) {
    return previousContext;
  }
  const token: AuthToken = JSON.parse(tokenJson);
  return {
    ...previousContext,
    headers: {
      ...previousContext.headers,
      Authorization: `Bearer ${token.accessToken}`,
    },
  };
});

// Link de errores globales
const errorLink = onError(function ({ graphQLErrors, networkError }) {
  if (graphQLErrors) {
    for (const err of graphQLErrors) {
      console.error(`[GraphQL Error]: ${err.message}`);
    }
  }
  if (networkError) {
    console.error(`[Network Error]: ${networkError.message}`);
  }
});

export const client = new ApolloClient({
  link: from([errorLink, authLink, httpLink]),
  cache: new InMemoryCache(),
});
