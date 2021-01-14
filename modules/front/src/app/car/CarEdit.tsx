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
  MainStore,
  useMainStore,
  useReaction,
  instanceItemToFormFields
} from "@cuba-platform/react-core";
import { Field, MultilineText, Spinner } from "@cuba-platform/react-ui";
import "../../app/App.css";
import { Car } from "../../cuba/entities/scr$Car";
import { Garage } from "../../cuba/entities/scr$Garage";
import { TechnicalCertificate } from "../../cuba/entities/scr$TechnicalCertificate";
import { FileDescriptor } from "../../cuba/entities/base/sys$FileDescriptor";
import {gql, useMutation, useLazyQuery} from "@apollo/client";
import { SerializedEntity, MetaClassInfo } from "@cuba-platform/rest";
import { getFields } from "./CarList";

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

const isNewEntity = (entityId: string) => {
  return entityId === NEW_SUBPATH;
};

// const getAssociationOptions = (
//   mainStore: MainStore
// ): CarEditAssociationOptions => {
//   const { getAttributePermission } = mainStore.security;
//   const associationOptions: CarEditAssociationOptions = {};
//
//   associationOptions.garagesDc = loadAssociationOptions(
//     Car.NAME,
//     "garage",
//     Garage.NAME,
//     getAttributePermission,
//     { view: "_minimal" }
//   );
//
//   associationOptions.technicalCertificatesDc = loadAssociationOptions(
//     Car.NAME,
//     "technicalCertificate",
//     TechnicalCertificate.NAME,
//     getAttributePermission,
//     { view: "_minimal" }
//   );
//
//   associationOptions.photosDc = loadAssociationOptions(
//     Car.NAME,
//     "photo",
//     FileDescriptor.NAME,
//     getAttributePermission,
//     { view: "_minimal" }
//   );
//
//   return associationOptions;
// };

function getEntityIdFieldName(entityName: String, metadata: MetaClassInfo[]): string {
  return 'id'; // TODO
}

const CAR_BY_ID = gql`
  query CarById($id: String!) {
      carById(id: $id) {
        id
        manufacturer
        model
        regNumber
        purchaseDate
        manufactureDate
        wheelOnRight
        carType
        ecoRank
        maxPassengers
        price
        mileage
      }
  }
`;

// TODO How to dynamically determine id field name?
const UPSERT_CAR = gql`
  mutation UpsertCar($car: inp_scr_Car!) {
    createCar(car: $car) {
      id
    }
  }
`;

const CarEdit = (props: Props) => {
  const { entityId } = props;

  const intl = useIntl();
  const mainStore = useMainStore();
  const [form] = useForm();

  const [doFetch, {loading, error, data}] = useLazyQuery(CAR_BY_ID);

  const [upsertCar] = useMutation(UPSERT_CAR);

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

  // useEffect(() => {
  //   if (isNewEntity(entityId)) {
  //     dataInstance.current.setItem(new Car());
  //   } else {
  //     dataInstance.current.load(entityId);
  //   }
  // }, [entityId, dataInstance]);

  // // Create a reaction that displays request failed error message
  // useReaction(
  //   () => dataInstance.current.status,
  //   () => {
  //     if (
  //       dataInstance.current.lastError != null &&
  //       dataInstance.current.lastError !== "COMMIT_ERROR"
  //     ) {
  //       message.error(intl.formatMessage({ id: "common.requestFailed" }));
  //     }
  //   }
  // );

  // Create a reaction that waits for permissions data to be loaded,
  // loads Association options and disposes itself
  // useReaction(
  //   () => mainStore.security.isDataLoaded,
  //   (isDataLoaded, permsReaction) => {
  //     if (isDataLoaded === true) {
  //       // User permissions has been loaded.
  //       // We can now load association options.
  //       const associationOptions = getAssociationOptions(mainStore); // Calls REST API
  //       Object.assign(store, associationOptions);
  //       permsReaction.dispose();
  //     }
  //   },
  //   { fireImmediately: true }
  // );

  // Create a reaction that sets the fields values based on dataInstance.current.item
  useReaction(
    () => [store.formRef.current, loading, error, data],
    ([formInstance]) => {
      if (formInstance != null && !loading && error == null) {
        form.setFieldsValue(data);
      }
    },
    { fireImmediately: true }
  );

  useEffect(() => {
    if (entityId != null && entityId !== 'new') {
      doFetch({
        variables: {
          id: entityId // TODO dynamic id field name
        }
      })
    }
  }, [entityId]);

  useEffect(() => {
    if (store.formRef.current != null && !loading && error == null && data != null) {
      form.setFieldsValue(jmix2ant<Car>(
        data.carById,
        Car.NAME,
        mainStore.metadata
      ));
    }
  }, [store.formRef.current, loading, error, data]);

  const handleFinishFailed = useCallback(() => {
    message.error(
      intl.formatMessage({ id: "management.editor.validationError" })
    );
  }, [intl]);

  const handleFinish = useCallback(
    (values: { [field: string]: any }) => {
      if (form != null && mainStore.metadata != null) {
        console.log('values', values);
        upsertCar({
          variables: {
            car: {
              ...values,
              ...addIdIfExistingEntity(entityId, mainStore.metadata)
            }
          }
        }).then(({errors}) => {
          if (errors == null || errors.length === 0) {
            store.updated = true;
          } else {
            console.error(errors); // TODO Error handling
          }
        });
      }
    },
    []
  );

  return useObserver(() => {
    if (store.updated) {
      return <Redirect to={PATH} />;
    }

    if (loading) {
      return <Spinner />;
    }

    if (error != null) {
      console.error(error);
      return (
        <>
          <FormattedMessage id="common.requestFailed" />.
          <br />
          <br />
          <Button htmlType="button" onClick={() => doFetch()}>
            <FormattedMessage id="common.retry" />
          </Button>
        </>
      );
    }

    // TODO errors
    // // do not stop on "COMMIT_ERROR" - it could be bean validation, so we should show fields with errors
    // if (status === "ERROR" && lastError === "LOAD_ERROR") {
    //   return (
    //     <>
    //       <FormattedMessage id="common.requestFailed" />.
    //       <br />
    //       <br />
    //       <Button htmlType="button" onClick={() => load(entityId)}>
    //         <FormattedMessage id="common.retry" />
    //       </Button>
    //     </>
    //   );
    // }

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
              // disabled={status !== "DONE" && status !== "ERROR"} TODO Client-side validation
              loading={loading}
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

function addIdIfExistingEntity(entityId: string, metadata: MetaClassInfo[]) {
  return entityId === 'new'
    ? undefined
    : {[getEntityIdFieldName(Car.NAME, metadata)]: entityId};
}

// TODO get rid of any and !
// TODO move to react-ui
// TODO will get metadata via graphql in future?
function jmix2ant<T>(
  item: Record<string, any>,
  entityName: string,
  metadata?: MetaClassInfo[],
  stringIdName?: string
): Record<string, any> {
  const displayedProperties = getFields(item, stringIdName != null); // TODO

  // TODO Refactgor instanceItemToFormFields, move to react-ui
  return instanceItemToFormFields(item, entityName, metadata!, displayedProperties, stringIdName)
}

// TODO
// function ant2jmix() {
//
// }

export default CarEdit;
