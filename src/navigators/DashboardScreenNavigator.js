import { StackNavigator } from 'react-navigation';
import FilesScreen from '../containers/FilesListContainer';
import DashboardScreen from '../containers/DashboardContainer';

const DashboardScreenNavigator = StackNavigator(
    {
        DashboardDefaultScreen: {
            screen: DashboardScreen,
            routeName: 'DashboardDefaultScreen'
        },
        FilesScreen: {
            screen: FilesScreen,
            routeName: 'DashboardFilesScreen'
        }
    }, 
    {
        initialRouteName: 'DashboardDefaultScreen',
        headerMode : 'none'
    }
);

export default DashboardScreenNavigator;