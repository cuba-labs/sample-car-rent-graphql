import {ApolloClient, InMemoryCache} from '@apollo/client';
import { GQL_URI } from './config';
import {TypePolicies} from '@apollo/client/cache/inmemory/policies';

export const createApolloClient = () => {
  return new ApolloClient({
    uri: GQL_URI,
    cache: new InMemoryCache({
      typePolicies: createTypePolicies()
    })
  })
}

function createTypePolicies(): TypePolicies {
  return {
    ...createLocalTypePolicies()
  };
}

function createLocalTypePolicies() {
  return {
    Query: {
      fields: {
        testLocalState: {
          read() {
            return '[some local state obtained via Apollo]';
          }
        }
      }
    }
  };
}
