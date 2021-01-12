import {ApolloClient, InMemoryCache} from '@apollo/client';
import { GQL_URI } from './config';

export const createApolloClient = () => {
  return new ApolloClient({
    uri: GQL_URI,
    cache: new InMemoryCache({
      typePolicies: {
        StringIdEntity: {
          keyFields: ['identifier']
        }
      }
    }),
    defaultOptions: {
      watchQuery: {
        fetchPolicy: 'cache-and-network',
      }
    }
  })
}
