import * as React from "react";
import { observer } from "mobx-react";
import { Link } from "react-router-dom";
import { IReactionDisposer, reaction } from "mobx";
import {Modal, Button, Card, Icon, message, Input} from "antd";
import {
  injectMainStore,
  MainStoreInjected
} from "@cuba-platform/react-core";
import {
  EntityProperty,
  Paging,
  setPagination,
  Spinner
} from "@cuba-platform/react-ui";
import { Car } from "../../cuba/entities/scr$Car";
import {ConditionsGroup, SerializedEntity} from "@cuba-platform/rest";
import { CarCrud } from "./CarCrud";
import {
  FormattedMessage,
  injectIntl,
  WrappedComponentProps
} from "react-intl";
import { PaginationConfig } from "antd/es/pagination";
import {ApolloClient, gql, InMemoryCache, useQuery} from '@apollo/client';
import {GroupCondition} from '../../cuba/entities/base/sec$GroupCondition';
import Search from 'antd/es/input/Search';
import {useState} from 'react';

const CAR_LIST = gql`
    query CarList($filter: GroupCondition, $limit: Int, $offset: Int, $sort: String) {
        carList(filter: $filter, limit: $limit, offset: $offset, sort: $sort) {
            manufacturer
            model
        }
    }
`;

const CarList = (props: {
    paginationConfig: PaginationConfig;
    onPagingChange: (current: number, pageSize: number) => void;
  }) => {

  // const [filter, setFilter] = useState<ConditionsGroup>();

  const {loading, error, data} = useQuery(CAR_LIST, {
    variables: {
      filter: {
        group: 'AND',
        conditions: [
          {
            property: 'manufacturer',
            operator: '=',
            value: 'Tata'
          }
        ]
      }
    }
  });

  if (loading) {
    return <>Loading...</>;
  }

  if (error) {
    return <>Error :(</>;
  }

  return (
    <>
      {/*<Search placeholder='Filter by manufacturer'*/}
      {/*        key='search'*/}
      {/*        onSearch={val => setFilter({*/}
      {/*          group: 'AND',*/}
      {/*          conditions: [*/}
      {/*            {*/}
      {/*              property: 'manufacturer',*/}
      {/*              operator: '=',*/}
      {/*              value: val*/}
      {/*            }*/}
      {/*          ]*/}
      {/*        })}*/}
      {/*/>*/}
      {data.carList.map((car: Partial<Car>, index: number) => <p key={index}>{JSON.stringify(car)}<br/></p>)}
    </>
  );
};

export default CarList;

// type Props = MainStoreInjected &
//   WrappedComponentProps & {
//     paginationConfig: PaginationConfig;
//     onPagingChange: (current: number, pageSize: number) => void;
//   };
// @injectMainStore
// @observer
// class CarListComponent extends React.Component<Props> {
//   // client = new ApolloClient({
//   //   uri: 'http://localhost:8080/app-portal/graphql',
//   //   cache: new InMemoryCache()
//   // });
//
//   reactionDisposers: IReactionDisposer[] = [];
//   fields = [
//     "manufacturer",
//     "model",
//     "regNumber",
//     "purchaseDate",
//     "manufactureDate",
//     "wheelOnRight",
//     "carType",
//     "ecoRank",
//     "maxPassengers",
//     "price",
//     "mileage",
//     "garage",
//     "technicalCertificate",
//     "photo"
//   ];
//
//   componentDidMount(): void {
//     // this.reactionDisposers.push(
//     //   reaction(
//     //     () => this.props.paginationConfig,
//     //     paginationConfig =>
//     //       setPagination(paginationConfig, this.dataCollection, true)
//     //   )
//     // );
//     // setPagination(this.props.paginationConfig, this.dataCollection, true);
//     //
//     // this.reactionDisposers.push(
//     //   reaction(
//     //     () => this.dataCollection.status,
//     //     status => {
//     //       const { intl } = this.props;
//     //       if (status === "ERROR") {
//     //         message.error(intl.formatMessage({ id: "common.requestFailed" }));
//     //       }
//     //     }
//     //   )
//     // );
//   }
//
//   componentWillUnmount() {
//     this.reactionDisposers.forEach(dispose => dispose());
//   }
//
//   showDeletionDialog = (e: SerializedEntity<Car>) => {
//     Modal.confirm({
//       title: this.props.intl.formatMessage(
//         { id: "management.browser.delete.areYouSure" },
//         { instanceName: e._instanceName }
//       ),
//       okText: this.props.intl.formatMessage({
//         id: "management.browser.delete.ok"
//       }),
//       cancelText: this.props.intl.formatMessage({ id: "common.cancel" }),
//       onOk: () => {
//         // return this.dataCollection.delete(e);
//       }
//     });
//   };
//
//   render() {
//     return null;
//     // this.client.query({
//     //   query: gql`
//     //     {
//     //       carList {
//     //         manufacturer
//     //         model
//     //       }
//     //     }
//     //   `
//     // }).then(result => console.log(result));
//
//     // const { status, items, count } = this.dataCollection;
//     // const { paginationConfig, onPagingChange, mainStore } = this.props;
//     //
//     // if (status === "LOADING" || mainStore?.isEntityDataLoaded() !== true) {
//     //   return <Spinner />;
//     // }
//     //
//     // return (
//     //   <div className="narrow-layout">
//     //     <div style={{ marginBottom: "12px" }}>
//     //       <Link to={CarCrud.PATH + "/" + CarCrud.NEW_SUBPATH}>
//     //         <Button htmlType="button" type="primary" icon="plus">
//     //           <span>
//     //             <FormattedMessage id="common.create" />
//     //           </span>
//     //         </Button>
//     //       </Link>
//     //     </div>
//     //
//     //     {items == null || items.length === 0 ? (
//     //       <p>
//     //         <FormattedMessage id="management.browser.noItems" />
//     //       </p>
//     //     ) : null}
//     //     {items.map(e => (
//     //       <Card
//     //         title={e._instanceName}
//     //         key={e.id ? e.id : undefined}
//     //         style={{ marginBottom: "12px" }}
//     //         actions={[
//     //           <Icon
//     //             type="delete"
//     //             key="delete"
//     //             onClick={() => this.showDeletionDialog(e)}
//     //           />,
//     //           <Link to={CarCrud.PATH + "/" + e.id} key="edit">
//     //             <Icon type="edit" />
//     //           </Link>
//     //         ]}
//     //       >
//     //         {this.fields.map(p => (
//     //           <EntityProperty
//     //             entityName={Car.NAME}
//     //             propertyName={p}
//     //             value={e[p]}
//     //             key={p}
//     //           />
//     //         ))}
//     //       </Card>
//     //     ))}
//     //
//     //     {!this.props.paginationConfig.disabled && (
//     //       <div style={{ margin: "12px 0 12px 0", float: "right" }}>
//     //         <Paging
//     //           paginationConfig={paginationConfig}
//     //           onPagingChange={onPagingChange}
//     //           total={count}
//     //         />
//     //       </div>
//     //     )}
//     //   </div>
//     // );
//   }
// }
//
// const CarList = injectIntl(CarListComponent);
//
// export default CarList;
