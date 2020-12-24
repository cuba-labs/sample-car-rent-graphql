import React, { useCallback } from "react";
import { RouteComponentProps } from "react-router";
import { useLocalStore, useObserver } from "mobx-react";
import CarEdit from "./CarEdit";
import CarList from "./CarList";
import { action } from "mobx";
import { PaginationConfig } from "antd/es/pagination";
import { addPagingParams, createPagingConfig } from "@cuba-platform/react-ui";

type Props = RouteComponentProps<{ entityId?: string }>;

type CarCrudLocalStore = {
  paginationConfig: PaginationConfig;
};

export const PATH = "/carCrud";
export const NEW_SUBPATH = "new";

export const CarCrud = (props: Props) => {
  const { entityId } = props.match.params;

  const store: CarCrudLocalStore = useLocalStore(() => ({
    paginationConfig: createPagingConfig(props.location.search)
  }));

  const onPagingChange = useCallback(
    action((current: number, pageSize: number) => {
      props.history.push(addPagingParams("carCrud", current, pageSize));
      store.paginationConfig = { ...store.paginationConfig, current, pageSize };
    }),
    []
  );

  return useObserver(() => {
    return entityId != null ? (
      <CarEdit entityId={entityId} />
    ) : (
      <CarList
        onPagingChange={onPagingChange}
        paginationConfig={store.paginationConfig}
      />
    );
  });
};
