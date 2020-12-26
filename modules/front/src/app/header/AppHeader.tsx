import { LogoutOutlined } from "@ant-design/icons";
import { Button, Modal } from "antd";
import * as React from "react";
import { observer } from "mobx-react";
import "./AppHeader.css";
import logo from "./logo.png";
import { injectMainStore, MainStoreInjected } from "@cuba-platform/react-core";
import { LanguageSwitcher } from "../../i18n/LanguageSwitcher";
import { injectIntl, WrappedComponentProps } from "react-intl";
import {gql, useQuery} from "@apollo/client";

const TEST_LOCAL_STATE = gql`
  query TestLocalState {
      testLocalState @client
  }
`;

const TestLocalState: React.FC = () => {
  const {loading, error, data} = useQuery(TEST_LOCAL_STATE);

  if (loading) {
    return <>Loading...</>;
  }
  if (error != null) {
    return <>Error</>;
  }

  console.log('data', data);

  return <>{data?.testLocalState}</>;
};

@injectMainStore
@observer
class AppHeader extends React.Component<
  MainStoreInjected & WrappedComponentProps
> {
  render() {
    const appState = this.props.mainStore!;

    return (
      <div className="app-header">
        <div>
          <img
            src={logo}
            alt={this.props.intl.formatMessage({ id: "common.alt.logo" })}
          />
        </div>
        <div className="user-panel">
          <LanguageSwitcher className="panelelement language-switcher -header" />
          <span className="panelelement">{appState.userName}</span>
          <TestLocalState/>
          <Button
            className="panelelement"
            id="button_logout"
            ghost={true}
            icon={<LogoutOutlined />}
            onClick={this.showLogoutConfirm}
          />
        </div>
      </div>
    );
  }

  showLogoutConfirm = () => {
    Modal.confirm({
      title: this.props.intl.formatMessage({ id: "header.logout.areYouSure" }),
      okText: this.props.intl.formatMessage({ id: "header.logout.ok" }),
      cancelText: this.props.intl.formatMessage({ id: "header.logout.cancel" }),
      onOk: () => {
        this.props.mainStore!.logout();
      }
    });
  };
}

export default injectIntl(AppHeader);
