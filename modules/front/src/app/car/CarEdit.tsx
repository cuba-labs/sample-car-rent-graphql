import React, { useCallback, useEffect, RefObject } from "react";
import { Form, Alert, Button, Card, message } from "antd";
import { FormInstance } from "antd/es/form";
import useForm from "antd/lib/form/hooks/useForm";
import { useLocalStore, useObserver } from "mobx-react";
import { PATH } from "./CarCrud";
import { Link, Redirect } from "react-router-dom";
import { toJS } from "mobx";
import { FormattedMessage, useIntl } from "react-intl";
import {
  createAntdFormValidationMessages
} from "@cuba-platform/react-ui";
import {
  DataCollectionStore,
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
import { MetaClassInfo } from "@cuba-platform/rest";
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

// const isNewEntity = (entityId: string) => {
//   return entityId === NEW_SUBPATH;
// };

// const GARAGE_OPTIONS = gql`
//   query GarageList {
//     instanceName
//     id
//   }
// `;

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
  return 'id'; // TODO determine the correct name for id field based on metadata
}

// The list of fields is passed from Stufio
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

// NOTE: we will replace a single "upsert"-like mutation with separate "create" and "update" mutations
// TODO A custom directive to dynamically determine the name of id field
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

  const [getCar, {loading: queryLoading, error: queryError, data: carData}] = useLazyQuery(CAR_BY_ID);

  const [upsertCar, {loading: upsertLoading}] = useMutation(UPSERT_CAR);

  // const [getGarageOptions, {
  //   loading: garageOptionsLoading,
  //   error: garageOptionsError,
  //   data: garageOptionsData
  // }] = useLazyQuery(GARAGE_OPTIONS);

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

  // Create a reaction that sets the fields values based on query data
  useReaction(
    () => [store.formRef.current, queryLoading, queryError, carData],
    ([formInstance]) => {
      if (formInstance != null && !queryLoading && queryError == null) {
        form.setFieldsValue(carData);
      }
    },
    { fireImmediately: true }
  );

  useEffect(() => {
    if (entityId != null && entityId !== 'new') {
      getCar({
        variables: {
          id: entityId // TODO dynamic id field name
        }
      })
    }
  }, [entityId, getCar]);

  useEffect(() => {
    if (store.formRef.current != null && !queryLoading && queryError == null && carData != null) {
      form.setFieldsValue(jmix2ant<Car>(
        carData.carById,
        Car.NAME,
        mainStore.metadata
      ));
    }
  }, [queryLoading, queryError, carData, form, mainStore.metadata, store.formRef]);

  const handleFinishFailed = useCallback(() => {
    message.error(
      intl.formatMessage({ id: "management.editor.validationError" })
    );
  }, [intl]);

  const handleFinish = useCallback(
    (values: { [field: string]: any }) => {
      if (form != null && mainStore.metadata != null) {
        upsertCar({
          variables: {
            car: {
              ...values,
              ...addIdIfExistingEntity(entityId, mainStore.metadata) // This will be refactored once we move to separate create/update mutations
            }
          }
        }).then(({errors}) => {
          if (errors == null || errors.length === 0) {
            store.updated = true;
          } else {
            console.error(errors); // TODO Error handling
          }
        }).catch(e => {
          console.error(e);
        });
      }
    },
    [entityId, form, mainStore.metadata, store.updated, upsertCar]
  );

  return useObserver(() => {
    if (store.updated) {
      return <Redirect to={PATH} />;
    }

    if (queryLoading) {
      return <Spinner />;
    }

    if (queryError != null) {
      console.error(queryError);
      return (
        <>
          <FormattedMessage id="common.requestFailed" />.
          <br />
          <br />
          <Button htmlType="button" onClick={() => getCar()}>
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
              loading={upsertLoading}
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
