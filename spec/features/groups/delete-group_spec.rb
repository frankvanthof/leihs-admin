require 'spec_helper'
require 'pry'

feature 'Manage groups', type: :feature do


  context 'an admin user and a bunch of other users and one group' do

    before :each do
      @admins = 3.times.map do
        FactoryBot.create :admin
      end.to_set

      @admin = @admins.first

      @users = 15.times.map do
        FactoryBot.create :user
      end.to_set

      @group = FactoryBot.create :group

      sign_in_as @admin
    end


    scenario "delete a group" do
      visit '/admin/'
      click_on 'Groups'
      click_on @group.name
      click_on 'Delete'
      wait_until do
        page.has_content? "Delete Group #{@group.name}"
      end
      click_on 'Delete'
      wait_until do
        page.has_content? "No (more) groups found."
      end

    end

  end

end
