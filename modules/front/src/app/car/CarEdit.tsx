import React, { useCallback, useEffect, RefObject } from "react";
import { Form, Alert, Button, Card, message } from "antd";
import { FormInstance } from "antd/es/form";
import useForm from "antd/lib/form/hooks/useForm";
import { useLocalStore, useObserver } from "mobx-react";
import { PATH, NEW_SUBPATH } from "./CarCrud";
import { Link, Redirect } from "react-router-dom";
import { toJS } from "mobx";
import { FormattedMessage, useIntl } from "react-intl";
import {
  defaultHandleFinish,
  createAntdFormValidationMessages
} from "@cuba-platform/react-ui";
import {
  loadAssociationOptions,
  DataCollectionStore,
  useInstance,
  MainStore,
  useMainStore,
  useReaction
} from "@cuba-platform/react-core";
import { Field, MultilineText, Spinner } from "@cuba-platform/react-ui";
import "../../app/App.css";
import { Car } from "../../cuba/entities/scr$Car";
import { Garage } from "../../cuba/entities/scr$Garage";
import { TechnicalCertificate } from "../../cuba/entities/scr$TechnicalCertificate";
import { FileDescriptor } from "../../cuba/entities/base/sys$FileDescriptor";

type Props = {
  entityId: string;
};

type CarEditAssociationOptions = {
  garagesDc?: DataCollectionStore<Garage>;
  technicalCertificatesDc?: DataCollectionStore<TechnicalCertificate>;
  photosDc?: DataCollectionStore<FileDescriptor>;
};

type CarEditLocalStore = CarEditAssociationOptions & {
  updated: boolean;
  globalErrors: string[];
  formRef: RefObject<FormInstance>;
};

const FIELDS = [
  "manufacturer",
  "model",
  "regNumber",
  "purchaseDate",
  "manufactureDate",
  "wheelOnRight",
  "carType",
  "ecoRank",
  "maxPassengers",
  "price",
  "mileage",
  "garage",
  "technicalCertificate",
  "photo"
];

const isNewEntity = (entityId: string) => {
  return entityId === NEW_SUBPATH;
};

const getAssociationOptions = (
  mainStore: MainStore
): CarEditAssociationOptions => {
  const { getAttributePermission } = mainStore.security;
  const associationOptions: CarEditAssociationOptions = {};

  associationOptions.garagesDc = loadAssociationOptions(
    Car.NAME,
    "garage",
    Garage.NAME,
    getAttributePermission,
    { view: "_minimal" }
  );

  associationOptions.technicalCertificatesDc = loadAssociationOptions(
    Car.NAME,
    "technicalCertificate",
    TechnicalCertificate.NAME,
    getAttributePermission,
    { view: "_minimal" }
  );

  associationOptions.photosDc = loadAssociationOptions(
    Car.NAME,
    "photo",
    FileDescriptor.NAME,
    getAttributePermission,
    { view: "_minimal" }
  );

  return associationOptions;
};

const CarEdit = (props: Props) => {
  const { entityId } = props;

  const intl = useIntl();
  const mainStore = useMainStore();
  const [form] = useForm();

  const dataInstance = useInstance<Car>(Car.NAME, {
    view: "car-edit",
    loadImmediately: false
  });

  const store: CarEditLocalStore = useLocalStore(() => ({
    // Association options
    garagesDc: undefined,
    technicalCertificatesDc: undefined,
    photosDc: undefined,

    // Other
    updated: false,
    globalErrors: [],
    formRef: React.createRef()
  }));

  useEffect(() => {
    if (isNewEntity(entityId)) {
      dataInstance.current.setItem(new Car());
    } else {
      dataInstance.current.load(entityId);
    }
  }, [entityId, dataInstance]);

  // Create a reaction that displays request failed error message
  useReaction(
    () => dataInstance.current.status,
    () => {
      if (
        dataInstance.current.lastError != null &&
        dataInstance.current.lastError !== "COMMIT_ERROR"
      ) {
        message.error(intl.formatMessage({ id: "common.requestFailed" }));
      }
    }
  );

  // Create a reaction that waits for permissions data to be loaded,
  // loads Association options and disposes itself
  useReaction(
    () => mainStore.security.isDataLoaded,
    (isDataLoaded, permsReaction) => {
      if (isDataLoaded === true) {
        // User permissions has been loaded.
        // We can now load association options.
        const associationOptions = getAssociationOptions(mainStore); // Calls REST API
        Object.assign(store, associationOptions);
        permsReaction.dispose();
      }
    },
    { fireImmediately: true }
  );

  // Create a reaction that sets the fields values based on dataInstance.current.item
  useReaction(
    () => [store.formRef.current, dataInstance.current.item],
    ([formInstance]) => {
      if (formInstance != null) {
        form.setFieldsValue(dataInstance.current.getFieldValues(FIELDS));
      }
    },
    { fireImmediately: true }
  );

  const handleFinishFailed = useCallback(() => {
    message.error(
      intl.formatMessage({ id: "management.editor.validationError" })
    );
  }, [intl]);

  const handleFinish = useCallback(
    (values: { [field: string]: any }) => {
      if (form != null) {
        defaultHandleFinish(
          values,
          dataInstance.current,
          intl,
          form,
          isNewEntity(entityId) ? "create" : "edit"
        ).then(({ success, globalErrors }) => {
          if (success) {
            store.updated = true;
          } else {
            store.globalErrors = globalErrors;
          }
        });
      }
    },
    [entityId, intl, form, store.globalErrors, store.updated, dataInstance]
  );

  return useObserver(() => {
    if (store.updated) {
      return <Redirect to={PATH} />;
    }

    if (!mainStore.isEntityDataLoaded()) {
      return <Spinner />;
    }

    const { status, lastError, load } = dataInstance.current;

    // do not stop on "COMMIT_ERROR" - it could be bean validation, so we should show fields with errors
    if (status === "ERROR" && lastError === "LOAD_ERROR") {
      return (
        <>
          <FormattedMessage id="common.requestFailed" />.
          <br />
          <br />
          <Button htmlType="button" onClick={() => load(entityId)}>
            <FormattedMessage id="common.retry" />
          </Button>
        </>
      );
    }

    return (
      <Card className="narrow-layout">
        <Form
          onFinish={handleFinish}
          onFinishFailed={handleFinishFailed}
          layout="vertical"
          ref={store.formRef}
          form={form}
          validateMessages={createAntdFormValidationMessages(intl)}
        >
          <Field
            entityName={Car.NAME}
            propertyName="manufacturer"
            formItemProps={{
              style: { marginBottom: "12px" },
              rules: [{ required: true }]
            }}
          />

          <Field
            entityName={Car.NAME}
            propertyName="model"
            formItemProps={{
              style: { marginBottom: "12px" }
            }}
          />

          <Field
            entityName={Car.NAME}
            propertyName="regNumber"
            formItemProps={{
              style: { marginBottom: "12px" }
            }}
          />

          <Field
            entityName={Car.NAME}
            propertyName="purchaseDate"
            formItemProps={{
              style: { marginBottom: "12px" }
            }}
          />

          <Field
            entityName={Car.NAME}
            propertyName="manufactureDate"
            formItemProps={{
              style: { marginBottom: "12px" }
            }}
          />

          <Field
            entityName={Car.NAME}
            propertyName="wheelOnRight"
            formItemProps={{
              style: { marginBottom: "12px" },
              valuePropName: "checked"
            }}
          />

          <Field
            entityName={Car.NAME}
            propertyName="carType"
            formItemProps={{
              style: { marginBottom: "12px" },
              rules: [{ required: true }]
            }}
          />

          <Field
            entityName={Car.NAME}
            propertyName="ecoRank"
            formItemProps={{
              style: { marginBottom: "12px" }
            }}
          />

          <Field
            entityName={Car.NAME}
            propertyName="maxPassengers"
            formItemProps={{
              style: { marginBottom: "12px" }
            }}
          />

          <Field
            entityName={Car.NAME}
            propertyName="price"
            formItemProps={{
              style: { marginBottom: "12px" }
            }}
          />

          <Field
            entityName={Car.NAME}
            propertyName="mileage"
            formItemProps={{
              style: { marginBottom: "12px" }
            }}
          />

          <Field
            entityName={Car.NAME}
            propertyName="garage"
            optionsContainer={store.garagesDc}
            formItemProps={{
              style: { marginBottom: "12px" }
            }}
          />

          <Field
            entityName={Car.NAME}
            propertyName="technicalCertificate"
            optionsContainer={store.technicalCertificatesDc}
            formItemProps={{
              style: { marginBottom: "12px" }
            }}
          />

          <Field
            entityName={Car.NAME}
            propertyName="photo"
            optionsContainer={store.photosDc}
            formItemProps={{
              style: { marginBottom: "12px" }
            }}
          />

          {store.globalErrors.length > 0 && (
            <Alert
              message={<MultilineText lines={toJS(store.globalErrors)} />}
              type="error"
              style={{ marginBottom: "24px" }}
            />
          )}

          <Form.Item style={{ textAlign: "center" }}>
            <Link to={PATH}>
              <Button htmlType="button">
                <FormattedMessage id="common.cancel" />
              </Button>
            </Link>
            <Button
              type="primary"
              htmlType="submit"
              disabled={status !== "DONE" && status !== "ERROR"}
              loading={status === "LOADING"}
              style={{ marginLeft: "8px" }}
            >
              <FormattedMessage id="common.submit" />
            </Button>
          </Form.Item>
        </Form>
      </Card>
    );
  });
};

export default CarEdit;
