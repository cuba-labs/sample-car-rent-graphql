import React, { useCallback } from "react";
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
import {gql, useQuery, useMutation} from '@apollo/client';
import {CenteredLoader} from '../CenteredLoader';

type Props = {
  paginationConfig: PaginationConfig;
  onPagingChange: (current: number, pageSize: number) => void;
};

const CAR_LIST = gql`
    query CarList($filter: GroupCondition, $limit: Int, $offset: Int, $sort: String) {
        carList(filter: $filter, limit: $limit, offset: $offset, sort: $sort) {
            id
            manufacturer
            model
        }
    }
`;

const DELETE_CAR = gql`
  mutation DeleteCar($id: UUID!) {
      deleteCar(id: $id)
  }
`;

const CarList = (props: Props) => {
  const { paginationConfig, onPagingChange } = props;

  const intl = useIntl();

  let limit = 10;
  let offset = 0;
  const {loading, error, data} = useQuery(CAR_LIST, {
    variables: {
      limit,
      offset
    }
  });

  const [deleteCar] = useMutation(DELETE_CAR);

  // TODO implement pagination

  // TODO deletion mutation
  const showDeletionDialog = useCallback(
    (e: SerializedEntity<Car>) => {
      console.log('e', e);
      Modal.confirm({
        title: intl.formatMessage(
          { id: "management.browser.delete.areYouSure" },
          { instanceName: e._instanceName }
        ),
        okText: intl.formatMessage({
          id: "management.browser.delete.ok"
        }),
        cancelText: intl.formatMessage({ id: "common.cancel" }),
        onOk: () => {
          if (e.id != null) {
            console.log('wtf?', e.id);
            deleteCar({variables: {id: e.id}});
          }
        }
      });
    },
    [intl, deleteCar]
  );

  return useObserver(() => {
    if (loading) {
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
              // total={count}
            />
          </div>
        )}
      </div>
    );
  });
};

// TODO Move to react-core?
export function getFields(item: SerializedEntity<Car>, isStringEntity: boolean): string[] {
  const ignoredProperties = ['__typename'];
  if (!isStringEntity) {
    ignoredProperties.push('id');
  }
  return Object.keys(item)
    .filter(key => !ignoredProperties.includes(key));
}

export default CarList;
