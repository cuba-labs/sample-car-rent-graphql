import React, { useCallback, useEffect } from "react";
import { useObserver } from "mobx-react";
import { Link } from "react-router-dom";
import { DeleteOutlined, EditOutlined, PlusOutlined } from "@ant-design/icons";
import { Modal, Button, List, message } from "antd";
import {
  EntityProperty,
  Paging,
} from "@cuba-platform/react-ui";
import { Car } from "../../cuba/entities/scr$Car";
import { SerializedEntity } from "@cuba-platform/rest";
import { PATH, NEW_SUBPATH } from "./CarCrud";
import { FormattedMessage, useIntl } from "react-intl";
import { PaginationConfig } from "antd/es/pagination";
import {gql, useMutation, Reference, useLazyQuery} from '@apollo/client';
import {CenteredLoader} from '../CenteredLoader';

type Props = {
  paginationConfig: PaginationConfig;
  onPagingChange: (current: number, pageSize: number) => void;
};

// TODO instanceName will become _instanceName
const CAR_LIST = gql`
    query CarList($filter: GroupCondition, $limit: Int, $offset: Int, $sort: String) {
        carCount
        carList(filter: $filter, limit: $limit, offset: $offset, sort: $sort) {
            instanceName
            id
            manufacturer
            model
        }
    }
`;

const DELETE_CAR = gql`
  mutation DeleteCar($id: String!) {
      deleteCar(id: $id)
  }
`;

const CarList = (props: Props) => {
  const { paginationConfig, onPagingChange } = props;

  const intl = useIntl();

  const [doFetch, {loading, error, data}] = useLazyQuery(CAR_LIST);
  const [deleteCar] = useMutation(DELETE_CAR);

  useEffect(() => {
    doFetch({
      variables: toLimitAndOffset(paginationConfig)
    });
  }, [paginationConfig]);

  // TODO deletion mutation
  const showDeletionDialog = useCallback(
    (e: SerializedEntity<Car>) => {
      console.log('e', e);
      Modal.confirm({
        title: intl.formatMessage(
          { id: "management.browser.delete.areYouSure" },
          // TODO instanceName will become _instanceName
          { instanceName: (e as any).instanceName }
        ),
        okText: intl.formatMessage({
          id: "management.browser.delete.ok"
        }),
        cancelText: intl.formatMessage({ id: "common.cancel" }),
        onOk: () => {
          if (e.id != null) {
            deleteCar({
              variables: {id: e.id},
              update(cache) {
                cache.modify({
                  fields: {
                    carList(existingRefs, { readField }) {
                      return existingRefs.filter(
                        (ref: Reference) => e.id !== readField('id', ref)
                      );
                    }
                  }
                })
              }
            });
          }
        }
      });
    },
    [intl, deleteCar]
  );

  return useObserver(() => {
    if (loading || data == null) {
      return <CenteredLoader/>;
    }

    if (error != null) {
      message.error(intl.formatMessage({ id: "common.requestFailed" }));
    }

    return (
      <div className="narrow-layout">
        <div style={{ marginBottom: "12px" }}>
          <Link to={PATH + "/" + NEW_SUBPATH}>
            <Button htmlType="button" type="primary" icon={<PlusOutlined />}>
              <span>
                <FormattedMessage id="common.create" />
              </span>
            </Button>
          </Link>
        </div>

        <List
          itemLayout="horizontal"
          bordered
          dataSource={data.carList}
          renderItem={(item: SerializedEntity<Car>) => (
            <List.Item
              actions={[
                <DeleteOutlined
                  key="delete"
                  onClick={() => showDeletionDialog(item)}
                />,
                <Link to={PATH + "/" + item.id} key="edit">
                  <EditOutlined />
                </Link>
              ]}
            >
              <div style={{ flexGrow: 1 }}>
                {getFields(item, false).map((p => (
                  <EntityProperty
                    entityName={Car.NAME}
                    propertyName={p}
                    value={item[p]}
                    key={p}
                  />
                )))}
              </div>
            </List.Item>
          )}
        />

        {!paginationConfig.disabled && (
          <div style={{ margin: "12px 0 12px 0", float: "right" }}>
            <Paging
              paginationConfig={paginationConfig}
              onPagingChange={onPagingChange}
              total={data.carCount}
            />
          </div>
        )}
      </div>
    );
  });
};

function toLimitAndOffset(paginationConfig: PaginationConfig): {limit: number | undefined, offset: number | undefined} {
  const {disabled, current, pageSize} = paginationConfig;

  if (disabled) {
    return {
      limit: undefined,
      offset: undefined
    };
  }

  if (pageSize != null && current != null) {
    return {
      limit: pageSize,
      offset: pageSize * (current - 1)
    }
  }

  return {
    limit: undefined,
    offset: undefined
  };
}

// TODO Move to react-core?
export function getFields(item: SerializedEntity<Car>, isStringEntity: boolean): string[] {
  // TODO instanceName will become _instanceName
  const ignoredProperties = ['__typename', 'instanceName'];
  if (!isStringEntity) {
    ignoredProperties.push('id');
  }
  return Object.keys(item)
    .filter(key => !ignoredProperties.includes(key));
}

export default CarList;
