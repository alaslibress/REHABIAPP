import { View, Pressable } from 'react-native';
import { AppText } from './AppText';

type SegmentOption = {
  label: string;
  value: string;
};

type SegmentedControlProps = {
  options: SegmentOption[];
  value: string;
  onChange: (v: string) => void;
};

// Control segmentado de seleccion unica en forma de pastillas
export function SegmentedControl(props: SegmentedControlProps) {
  const { options, value, onChange } = props;

  return (
    <View className="flex-row rounded-full bg-background dark:bg-background-dark p-1 gap-1">
      {options.map(function (opt) {
        const active = opt.value === value;
        return (
          <Pressable
            key={opt.value}
            onPress={function () { onChange(opt.value); }}
            className={`flex-1 py-2 px-3 rounded-full justify-center items-center ${
              active
                ? 'bg-primary-600'
                : 'bg-transparent'
            }`}
          >
            <AppText
              variant="label"
              weight={active ? 'semibold' : 'regular'}
              className={active ? 'text-white' : 'text-text-primary dark:text-text-primary-dark'}
            >
              {opt.label}
            </AppText>
          </Pressable>
        );
      })}
    </View>
  );
}
